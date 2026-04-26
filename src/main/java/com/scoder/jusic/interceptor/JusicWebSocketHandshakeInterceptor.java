package com.scoder.jusic.interceptor;

import com.scoder.jusic.util.IPUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * 自定义 WebSocket 握手拦截器
 *
 * @author H
 */
@Component
@Slf4j
public class JusicWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private static final String DEFAULT_ROOM_ID = "default";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        this.putIfPresent(attributes, "remoteAddress", IPUtils.getRemoteAddress(request));
        this.putIfPresent(attributes, "roomId", this.getRoomId(request));
        this.putIfPresent(attributes, "housePwd", this.getQueryValue(request, "housePwd"));
        this.putIfPresent(attributes, "connectType", this.getQueryValue(request, "connectType"));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }

    private String getRoomId(ServerHttpRequest request) {
        String roomId = this.getQueryValue(request, "roomId");
        if (roomId == null || "".equals(roomId.trim())) {
            roomId = this.getQueryValue(request, "houseId");
        }
        if (roomId == null || "".equals(roomId.trim())) {
            return DEFAULT_ROOM_ID;
        }
        return roomId;
    }

    private String getQueryValue(ServerHttpRequest request, String key) {
        String value = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst(key);
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private void putIfPresent(Map<String, Object> attributes, String key, String value) {
        if (value != null) {
            attributes.put(key, value);
        }
    }
}
