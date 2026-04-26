package com.scoder.jusic.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * 轻量房间信息。
 *
 * @author H
 */
@Data
@Builder(toBuilder = true)
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class House implements Serializable {

    private static final long serialVersionUID = 7948404771026158691L;

    private String id;

    private String name;

    private String desc;

    private HouseType roomType;

    private Boolean needPwd = false;

    private String password;

    private Boolean enableStatus = false;

    private String retainKey;

    private Integer population = 0;

    private Notice announce;

}
