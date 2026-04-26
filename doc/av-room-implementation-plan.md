# AV room implementation plan

## Goal

Evolve UNION from a single global "shared music" session into a room-based AV room with these target behaviors:

- 2 participants prefer P2P transport.
- The room can play music together and watch video together.
- Video sources may come from either participant's local media or from online media.
- Music sources should support NetEase and Bilibili.
- When participant count grows beyond the 2-person P2P case, or when P2P cannot be established, the room falls back to a server-hosted mode exposed on one server port.

## Prerequisites and decision gates

Implementation should not be finalized until these open questions are confirmed:

1. Whether P2P covers only control/signaling or also carries audio/video media via WebRTC.
2. Whether P2P failure should automatically switch the room into server mode.
3. What "local media" means in practice: browser-selected local file, direct upload, LAN path, or another source model.
4. Which online video sources are supported: Bilibili video, direct mp4/m3u8 URLs, other sites, etc.
5. Whether Bilibili support is audio-only for music, or also usable as a video source.
6. Whether server fallback is control-only plus HTTP playback, or true server-side media forwarding.
7. Whether 3+ participants always force server mode, and whether mode switches must be seamless.
8. Whether uploaded/local media is transient or persisted/cached on the server.

## Current codebase reuse map

### 1. Real-time signaling and session bootstrap

Best reuse candidates:

- `src/main/java/com/scoder/jusic/configuration/JusicWebSocketConfiguration.java`
- `src/main/java/com/scoder/jusic/handler/JusicWebSocketHandler.java`
- `src/main/java/com/scoder/jusic/handler/JusicWebSocketHandlerAsync.java`
- `src/main/java/com/scoder/jusic/interceptor/JusicWebSocketHandshakeInterceptor.java`
- `src/main/java/com/scoder/jusic/interceptor/JusicWebSocketInterceptor.java`
- `src/main/java/com/scoder/jusic/service/SessionService.java`
- `src/main/java/com/scoder/jusic/service/imp/SessionServiceImpl.java`

How to reuse:

- Keep `/server` as the control-plane websocket endpoint.
- Extend the existing connection bootstrap so a client receives a room-scoped snapshot instead of the current global ONLINE/MUSIC/PICK state.
- Reuse the existing `SessionService.send(...)` broadcast shape, but make it room-aware instead of global.
- Reuse the websocket channel as the signaling path for P2P negotiation if WebRTC is selected.

### 2. Redis-backed runtime state

Best reuse candidates:

- `src/main/java/com/scoder/jusic/configuration/JusicProperties.java`
- `src/main/java/com/scoder/jusic/repository/impl/SessionRepositoryImpl.java`
- `src/main/java/com/scoder/jusic/repository/impl/MusicPickRepositoryImpl.java`
- `src/main/java/com/scoder/jusic/repository/impl/MusicPlayingRepositoryImpl.java`
- `src/main/java/com/scoder/jusic/repository/impl/MusicVoteRepositoryImpl.java`
- `src/main/java/com/scoder/jusic/repository/impl/ConfigRepositoryImpl.java`
- `src/main/java/com/scoder/jusic/repository/impl/MusicBlackRepositoryImpl.java`
- `src/main/java/com/scoder/jusic/repository/impl/SessionBlackRepositoryImpl.java`

How to reuse:

- Preserve the Redis list/hash/set pattern already used for playing item, queue, votes, blacklists, and config.
- Replace current global keys with room-scoped keys such as `room:{roomId}:pick`, `room:{roomId}:playing`, `room:{roomId}:members`, `room:{roomId}:mode`.
- Keep server-port suffixing if needed, but add room-level isolation first.

### 3. Media control and synchronization

Best reuse candidates:

- `src/main/java/com/scoder/jusic/controller/MusicController.java`
- `src/main/java/com/scoder/jusic/job/MusicJob.java`
- `src/main/java/com/scoder/jusic/service/MusicService.java`
- `src/main/java/com/scoder/jusic/service/imp/MusicServiceImpl.java`
- `src/main/java/com/scoder/jusic/model/Music.java`
- `src/main/java/com/scoder/jusic/model/MessageType.java`

How to reuse:

- Reuse current queue, current-playing item, vote-to-skip, top/delete/ban logic as the first server-mode implementation.
- Generalize `Music` into a broader media model or add a parallel `Video`/`MediaItem` model while preserving compatible fields like duration, url, picture, source, pushTime.
- Reuse `MusicJob` scheduling logic conceptually, but move it from one global dispatcher to room-scoped dispatch.
- Extend `MessageType` rather than replacing the protocol format.

### 4. User identity, admin, chat

Best reuse candidates:

- `src/main/java/com/scoder/jusic/controller/AuthController.java`
- `src/main/java/com/scoder/jusic/controller/ConfigController.java`
- `src/main/java/com/scoder/jusic/controller/ChatController.java`
- `src/main/java/com/scoder/jusic/model/User.java`
- `src/main/java/com/scoder/jusic/service/imp/AuthServiceImpl.java`

How to reuse:

- Keep the same session/user/admin concepts.
- Make nickname, role, blacklist, and chat room-scoped where appropriate.
- Preserve moderation logic, but bind it to a room instead of the whole server.

## Important blocker: frontend source is missing here

The current repository only contains compiled frontend assets:

- `src/main/resources/static/index.html`
- `src/main/resources/static/js/*.js`
- `src/main/resources/static/css/*.css`

The original README points to a separate frontend repository. Meaningful AV-room UI work should happen in the actual frontend source, not by editing minified bundles in `static/js`.

## Proposed target architecture

### Control plane

Keep the websocket endpoint as the unified control plane:

- room create/join/leave
- media queue operations
- playback sync events
- P2P offer/answer/ICE signaling
- mode switch notifications (`P2P` <-> `SERVER`)

### Data plane

- **2-person room**: prefer direct peer media transport if requirements confirm WebRTC media.
- **Server mode**: preserve synchronized playback using server-side room state and one public server port.
- The backend should always remain the source of truth for room membership, active media item, and transport mode.

### Suggested new domain concepts

- `Room`
- `RoomMember`
- `RoomMode` (`P2P`, `SERVER`)
- `MediaItem`
- `MediaSourceType` (`NETEASE`, `BILIBILI_AUDIO`, `BILIBILI_VIDEO`, `LOCAL_FILE`, `DIRECT_URL`)
- `SyncState` (play/pause/currentTime/rate/volume/updatedBy)
- `SignalMessage` (offer/answer/ice)

## Recommended implementation phases

### Phase 0: lock requirements and source repos

1. Confirm the eight open requirement gaps above.
2. Obtain the real frontend source repository before UI implementation starts.
3. Decide whether the first milestone should ship server mode first, then add 2-person P2P, or build both together.

### Phase 1: introduce room-scoped backend state

Primary backend files to change first:

- `src/main/java/com/scoder/jusic/configuration/JusicProperties.java`
- `src/main/java/com/scoder/jusic/service/SessionService.java`
- `src/main/java/com/scoder/jusic/service/imp/SessionServiceImpl.java`
- `src/main/java/com/scoder/jusic/handler/JusicWebSocketHandlerAsync.java`
- `src/main/java/com/scoder/jusic/repository/impl/SessionRepositoryImpl.java`
- `src/main/java/com/scoder/jusic/repository/impl/MusicPickRepositoryImpl.java`
- `src/main/java/com/scoder/jusic/repository/impl/MusicPlayingRepositoryImpl.java`
- `src/main/java/com/scoder/jusic/repository/impl/MusicVoteRepositoryImpl.java`
- `src/main/java/com/scoder/jusic/repository/impl/ConfigRepositoryImpl.java`

Planned work:

- Add room identity to websocket session bootstrap and runtime session storage.
- Introduce room-aware Redis key generation.
- Ensure queue/playing/vote/config state is isolated by room.
- Add room snapshot payloads for initial sync.

### Phase 2: expand the protocol and controllers

Primary backend files:

- `src/main/java/com/scoder/jusic/model/MessageType.java`
- `src/main/java/com/scoder/jusic/controller/MusicController.java`
- `src/main/java/com/scoder/jusic/controller/ChatController.java`
- plus new room/signaling controllers if needed

Planned work:

- Add room lifecycle messages.
- Add explicit sync messages for play, pause, seek, rate, volume, source switch.
- Add websocket signaling messages for P2P negotiation.
- Keep current controller style, but split responsibilities so room control is not mixed into music-only commands.

### Phase 3: refactor media services from music-only to AV

Primary backend files:

- `src/main/java/com/scoder/jusic/service/MusicService.java`
- `src/main/java/com/scoder/jusic/service/imp/MusicServiceImpl.java`
- `src/main/java/com/scoder/jusic/model/Music.java`
- `src/main/java/com/scoder/jusic/job/MusicJob.java`

Planned work:

- Extract source-specific lookup logic behind adapter-style components.
- Keep NetEase lookup as one adapter.
- Use the existing QQ/MG branching pattern as a template for adding Bilibili and future media sources.
- Introduce a media abstraction that can represent either audio or video.
- Replace the global scheduler with room-scoped playback progression.

### Phase 4: add transport mode orchestration

Planned work:

- Track room member count and transport mode on the backend.
- Enter `P2P` when a room has exactly 2 participants and negotiation succeeds.
- Enter `SERVER` when negotiation fails, when a third participant joins, or when the selected source cannot be delivered peer-to-peer.
- Broadcast mode changes as part of room snapshot updates.

### Phase 5: frontend implementation

Because frontend source is not in this repo, this phase depends on the frontend repository.

Planned work:

- Add room creation/join flows.
- Add room snapshot hydration on connect/reconnect.
- Add signaling integration for 2-person P2P.
- Add source-selection UI for NetEase, Bilibili, local file, and online URL.
- Add playback controls that emit room-scoped sync events.
- Show current transport mode so users know whether the room is in P2P or server mode.

## Verification plan

### Backend verification

1. Room isolation
   - Two rooms can hold different queues, current items, and member lists without cross-talk.
2. Snapshot correctness
   - A reconnecting client receives the correct room state immediately after joining.
3. Mode switching
   - 2-person room enters P2P.
   - P2P negotiation failure falls back to server mode.
   - Adding a third participant switches the room to server mode.
4. Sync semantics
   - play, pause, seek, rate, volume, source changes are applied consistently.
5. Media source resolution
   - NetEase audio works.
   - Bilibili audio works.
   - Video source metadata resolves according to confirmed requirements.
6. Existing moderation/admin behavior
   - room-scoped blacklist, role checks, and delete/top actions still work.

### Manual end-to-end scenarios

1. Two users join one room and sync a NetEase music track.
2. Two users join one room and sync a Bilibili audio source.
3. Two users start local-video playback, then a third user joins and the room falls back to server mode.
4. One user disconnects and reconnects, and the room snapshot restores state.
5. Two separate rooms run different media simultaneously.

## Lowest-risk delivery sequence

If scope needs to be reduced, the safest delivery order is:

1. Room-scoped server mode for 2+ participants.
2. Generalized media model for music + online video.
3. Bilibili music source.
4. Local media support.
5. 2-person P2P optimization.

This sequence reuses the current architecture most effectively and avoids blocking the whole project on WebRTC or local-file transport decisions.
