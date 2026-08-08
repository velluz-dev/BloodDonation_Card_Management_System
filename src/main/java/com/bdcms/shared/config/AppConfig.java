package com.bdcms.shared.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Static application configuration read from {@code app.properties}.
 *
 * <p>This holds deployment concerns (where uploaded photos live, upload size
 * caps, BCrypt cost). Business rules that a centre may want to change at
 * runtime - notably the minimum donation interval - live in the {@code
 * settings} database table instead and are read through {@link SettingsService}.
 */
public final class AppConfig {

    private static final Properties PROPS = load();

    private AppConfig() {
    }

    /** Directory that donor photographs are written to. */
    public static Path uploadDir() {
        String configured = System.getProperty("bdcms.upload.dir",
                PROPS.getProperty("bdcms.upload.dir", "${user.home}/bdcms-uploads"));
        String expanded = configured.replace("${user.home}", System.getProperty("user.home"));
        return Path.of(expanded);
    }

    /** Maximum accepted donor photograph size in bytes. */
    public static long maxPhotoBytes() {
        return Long.parseLong(PROPS.getProperty("bdcms.upload.maxPhotoBytes", "2097152"));
    }

    /** BCrypt work factor for staff passwords and donor PINs. */
    public static int bcryptCost() {
        return Integer.parseInt(PROPS.getProperty("bdcms.security.bcryptCost", "12"));
    }

    /** Default centre name pre-filled on the donation form. */
    public static String defaultCentre() {
        return PROPS.getProperty("bdcms.centre.name", "Central Blood Bank");
    }

    public static String get(String key, String fallback) {
        return System.getProperty(key, PROPS.getProperty(key, fallback));
    }

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("app.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read app.properties", e);
        }
        return props;
    }
}
