package com.scoder.jusic.service;

import com.scoder.jusic.model.AvMediaResolveResult;

/**
 * @author H
 */
public interface AvMediaResolveService {

    AvMediaResolveResult resolve(String url, String title);
}
