package com.scoder.jusic.configuration;

import com.scoder.jusic.util.AvMediaStorage;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author H
 */
@Configuration
public class AvMediaWebConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        AvMediaStorage.ensureDirectory();
        registry.addResourceHandler("/av/media/files/**")
                .addResourceLocations(AvMediaStorage.mediaDir().toUri().toString());
    }
}
