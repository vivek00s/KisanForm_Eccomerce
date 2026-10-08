package com.kisanfarm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves the React single-page app for client-side routes.
 *
 * React Router owns paths like /products, /cart, /admin/login. Without this,
 * a direct visit or a browser refresh on those paths makes Spring look for a
 * physical file and return 404. Here we forward them to index.html so React
 * can render the right page.
 *
 * REST endpoints under /api/** are untouched, and real static assets
 * (/assets/**, favicon, etc.) are still served normally because the resource
 * handler resolves them before these view controllers.
 */
@Configuration
public class SpaForwardingConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Buyer routes
        registry.addViewController("/products").setViewName("forward:/index.html");
        registry.addViewController("/products/**").setViewName("forward:/index.html");
        registry.addViewController("/cart").setViewName("forward:/index.html");
        registry.addViewController("/checkout").setViewName("forward:/index.html");
        registry.addViewController("/order-success").setViewName("forward:/index.html");
        registry.addViewController("/login").setViewName("forward:/index.html");
        registry.addViewController("/verify").setViewName("forward:/index.html");

        // Admin routes
        registry.addViewController("/admin").setViewName("forward:/index.html");
        registry.addViewController("/admin/**").setViewName("forward:/index.html");
    }
}
