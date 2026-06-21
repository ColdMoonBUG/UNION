package com.scoder.jusic.model;

import com.scoder.jusic.util.StringUtils;
import lombok.*;

import java.io.Serializable;

/**
 * @author H
 */
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = -5508341219684417455L;

    /**
     * WebSocketServerSockJsSession 中的 session id
     */
    private String sessionId;
    /**
     * 用户名
     */
    private String name = "";
    /**
     * 昵称
     */
    private String nickName = "";
    /**
     * ip 地址
     */
    private String remoteAddress = "";
    /**
     * 角色
     */
    private String role = "default";
    /**
     * 所属房间
     */
    private String roomId = "default";
    /**
     * 最后时间..
     */
    private Long lastMessageTime;

    public String getNickName() {
        if (this.name != null && !"".equals(this.name.trim())) {
            return this.name.trim();
        }
        if (this.remoteAddress != null && !"".equals(this.remoteAddress.trim())) {
            return StringUtils.desensitizeIPV4(this.remoteAddress.trim());
        }
        return "匿名";
    }
}
