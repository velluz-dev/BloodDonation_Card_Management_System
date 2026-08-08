package com.bdcms.shared.web;

import com.bdcms.shared.config.AppConfig;
import com.bdcms.shared.db.Datasource;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Application start-up and shutdown.
 *
 * <p>Builds the connection pool once when the context starts (rather than
 * lazily on the first request, so a bad database configuration fails loudly at
 * deployment time), creates the photo upload directory, and closes the pool on
 * undeploy so redeploying in Tomcat does not leak connections.
 */
@WebListener
public class AppLifecycleListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // QR code and PDF rendering use java.awt; a servlet container has no display.
        System.setProperty("java.awt.headless", "true");

        Path uploads = AppConfig.uploadDir();
        try {
            Files.createDirectories(uploads);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory " + uploads, e);
        }
        sce.getServletContext().log("BDCMS upload directory: " + uploads.toAbsolutePath());

        Datasource.init();
        sce.getServletContext().log("BDCMS connection pool started.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        Datasource.close();
        sce.getServletContext().log("BDCMS connection pool stopped.");
    }
}
