package com.scoder.jusic.configuration;

import com.scoder.jusic.util.AvMediaStorage;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import java.io.IOException;

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

    @Bean
    public FilterRegistrationBean<Filter> avMediaInlineFilter() {
        FilterRegistrationBean<Filter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                chain.doFilter(request, response);
                if (response instanceof HttpServletResponse) {
                    HttpServletResponse resp = (HttpServletResponse) response;
                    resp.setHeader("Content-Disposition", "inline");
                }
            }
        });
        reg.addUrlPatterns("/av/media/files/*");
        reg.setOrder(1);
        return reg;
    }
}
