package com.bdcms.shared.config;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class AppConfigTest {

    @Test
    void uploadDirUsesHomeFolderWhenPropertyIsNotSet() {
        Path uploadDir = AppConfig.uploadDir();

        assertTrue(uploadDir.toString().contains(System.getProperty("user.home")));
        assertTrue(uploadDir.toString().endsWith("bdcms-uploads"));
    }

    @Test
    void maxPhotoBytesUsesConfiguredDefault() {
        assertEquals(2_097_152L, AppConfig.maxPhotoBytes());
    }

    @Test
    void bcryptCostUsesConfiguredDefault() {
        assertEquals(12, AppConfig.bcryptCost());
    }

    @Test
    void defaultCentreUsesConfiguredFallback() {
        assertEquals("Central Blood Bank", AppConfig.defaultCentre());
    }

    @Test
    void getReturnsSystemPropertyWhenPresent() {
        String original = System.setProperty("bdcms.centre.name", "Test Centre");
        try {
            assertEquals("Test Centre", AppConfig.get("bdcms.centre.name", "fallback"));
        } finally {
            if (original == null) {
                System.clearProperty("bdcms.centre.name");
            } else {
                System.setProperty("bdcms.centre.name", original);
            }
        }
    }
}
