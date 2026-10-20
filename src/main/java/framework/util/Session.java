package framework.util;

import java.util.HashMap;
import java.util.Map;


public class Session {

    private final Map<String, Object> values;
    private boolean invalidated;

    public Session() {
        this(new HashMap<>());
    }

    public Session(Map<String, Object> values) {
        this.values = values;
    }

    public void set(String key, Object value) {
        values.put(key, value);
    }

    public Object get(String key) {
        return values.get(key);
    }

    public <T> T get(String key, Class<T> type) {
        Object value = values.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public void remove(String key) {
        values.remove(key);
    }

    public void invalidate() {
        values.clear();
        invalidated = true;
    }

    public boolean isInvalidated() {
        return invalidated;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    @Override
    public String toString() {
        return "Session" + values + (invalidated ? " (invalidated)" : "");
    }
}
