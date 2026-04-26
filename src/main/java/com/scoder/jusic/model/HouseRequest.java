package com.scoder.jusic.model;

import lombok.Data;
import lombok.ToString;

/**
 * 房间创建/进入请求。
 *
 * @author H
 */
@Data
@ToString
public class HouseRequest {

    private String id;

    private String name;

    private String desc;

    private HouseType roomType;

    private Boolean needPwd = false;

    private String password;

    private Boolean enableStatus = false;

    private String retainKey;

}
