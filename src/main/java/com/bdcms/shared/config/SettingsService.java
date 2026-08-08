package com.bdcms.shared.config;

import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime-configurable business values, held in the {@code settings} table.
 *
 * <p>This is how the maintainability requirement is met: the minimum donation
 * interval is data, not a constant in {@code EligibilityService}. A centre can
 * change it in the admin screen and the rule changes with no redeployment.
 *
 * <p>Values are cached for a short TTL so that the eligibility check does not
 * issue an extra query on every donation. Methods are non-final so unit tests
 * can subclass with a fixed interval and run without a database.
 */
public class SettingsService {

    /** Minimum days between whole-blood donations. Proposal S10.1 default: 84 (12 weeks). */
    public static final String MIN_DONATION_INTERVAL_DAYS = "min_donation_interval_days";

    /** Default volume in millilitres pre-filled on the donation form. */
    public static final String DEFAULT_DONATION_VOLUME_ML = "default_donation_volume_ml";

    private static final long TTL_MILLIS = 30_000L;

    private final SettingsDao dao;
    private final Map<String, CachedValue> cache = new ConcurrentHashMap<>();

    public SettingsService() {
        this(new SettingsDao());
    }

    public SettingsService(SettingsDao dao) {
        this.dao = dao;
    }

    public int getInt(Connection conn, String key, int fallback) {
        String raw = getString(conn, key, null);
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public String getString(Connection conn, String key, String fallback) {
        CachedValue cached = cache.get(key);
        if (cached != null && !cached.isExpired()) {
            return cached.value == null ? fallback : cached.value;
        }
        String loaded = dao.find(conn, key).orElse(null);
        cache.put(key, new CachedValue(loaded));
        return loaded == null ? fallback : loaded;
    }

    public void save(Connection conn, String key, String value) {
        dao.upsert(conn, key, value);
        cache.remove(key);
    }

    public Map<String, String> all(Connection conn) {
        return dao.findAll(conn);
    }

    /** Drops the cache; used after a bulk settings update. */
    public void invalidate() {
        cache.clear();
    }

    private static final class CachedValue {
        private final String value;
        private final long loadedAt = System.currentTimeMillis();

        private CachedValue(String value) {
            this.value = value;
        }

        private boolean isExpired() {
            return System.currentTimeMillis() - loadedAt > TTL_MILLIS;
        }
    }
}
