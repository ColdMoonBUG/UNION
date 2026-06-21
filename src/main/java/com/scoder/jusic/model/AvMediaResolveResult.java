package com.scoder.jusic.model;

import lombok.Data;

/**
 * @author H
 */
@Data
public class AvMediaResolveResult {

    private String sourceType;

    private String originUrl;

    private String mediaUrl;

    private String title;

    private String contentType;

    private String mediaId;

    private String posterUrl;

    private Long resolvedAt;
}
