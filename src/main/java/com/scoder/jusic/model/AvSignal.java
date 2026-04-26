package com.scoder.jusic.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 房间 scoped 的 WebRTC 信令。
 *
 * @author H
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public class AvSignal extends Message {

    private String roomId;

    private String targetSessionId;

    private String signalType;

    private Object payload;

}
