package com.scoder.jusic.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 房间 scoped 的播放状态快照。
 *
 * @author H
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public class AvPlaybackState extends Message {

    private String roomId;

    private String mediaType;

    private String mediaId;

    private String mediaUrl;

    private String originUrl;

    private String sourceType;

    private String posterUrl;

    private String title;

    private Long positionMs;

    private Double playbackRate;

    private Double volume;

    private Long updatedAt;

    private Boolean playing;

    private Long resolvedAt;

    private String updatedBy;

}
