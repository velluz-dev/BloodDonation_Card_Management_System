package com.bdcms.shared.config;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingsServiceTest {

    @Test
    void getStringReturnsFallbackWhenValueIsMissing() {
        SettingsService service = new SettingsService(new StubSettingsDao());

        assertEquals("fallback", service.getString(null, "missing", "fallback"));
    }

    @Test
    void getIntReturnsFallbackWhenStoredValueIsNotNumeric() {
        SettingsService service = new SettingsService(new StubSettingsDao(Map.of("interval", "not-a-number")));

        assertEquals(42, service.getInt(null, "interval", 42));
    }

    @Test
    void saveInvalidatesCacheAndDelegatesToDao() {
        StubSettingsDao dao = new StubSettingsDao(Map.of("key", "value"));
        SettingsService service = new SettingsService(dao);

        assertEquals("value", service.getString(null, "key", "fallback"));
        service.save(null, "key", "updated");

        assertEquals("updated", service.getString(null, "key", "fallback"));
        assertEquals("updated", dao.lastSavedValue);
    }

    @Test
    void allDelegatesToDao() {
        Map<String, String> expected = Map.of("a", "1");
        SettingsService service = new SettingsService(new StubSettingsDao(expected));

        assertEquals(expected, service.all(null));
    }

    @Test
    void invalidateClearsCachedValues() {
        StubSettingsDao dao = new StubSettingsDao(Map.of("key", "value"));
        SettingsService service = new SettingsService(dao);

        assertEquals("value", service.getString(null, "key", "fallback"));
        service.invalidate();
        dao.values.put("key", "fresh");

        assertEquals("fresh", service.getString(null, "key", "fallback"));
    }

    private static final class StubSettingsDao extends SettingsDao {
        private final Map<String, String> values;
        private String lastSavedValue;

        private StubSettingsDao() {
            this(new LinkedHashMap<>());
        }

        private StubSettingsDao(Map<String, String> values) {
            this.values = new LinkedHashMap<>(values);
        }

        @Override
        public Optional<String> find(Connection conn, String key) {
            return Optional.ofNullable(values.get(key));
        }

        @Override
        public Map<String, String> findAll(Connection conn) {
            return new LinkedHashMap<>(values);
        }

        @Override
        public void upsert(Connection conn, String key, String value) {
            values.put(key, value);
            lastSavedValue = value;
        }
    }
}
