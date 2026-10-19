package framework.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Reads application.properties from the classpath (src/main/resources).
 * Values may contain ${placeholders}, resolved from JVM system properties
 * first, then environment variables, e.g. upload.dir=${user.home}/uploads
 */
public final class FrameworkConfig {

    public static final String DEFAULT_FILE = "application.properties";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    private final Properties properties;

    private FrameworkConfig(Properties properties) {
        this.properties = properties;
    }

    public static FrameworkConfig load() {
        return load(DEFAULT_FILE);
    }

    public static FrameworkConfig load(String resourceName) {
        Properties props = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = FrameworkConfig.class.getClassLoader();
        }
        try (InputStream in = loader.getResourceAsStream(resourceName)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + resourceName, e);
        }
        return new FrameworkConfig(props);
    }

    public String get(String key, String defaultValue) {
        String raw = properties.getProperty(key);
        return raw == null || raw.isBlank() ? defaultValue : resolvePlaceholders(raw.trim());
    }

    public long getLong(String key, long defaultValue) {
        String value = get(key, null);
        return value == null ? defaultValue : Long.parseLong(value);
    }

    public Set<String> getLowerCaseSet(String key, String defaultValue) {
        String value = get(key, defaultValue);
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String resolvePlaceholders(String value) {
        Matcher m = PLACEHOLDER.matcher(value);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            String replacement = System.getProperty(name, System.getenv(name));
            if (replacement == null) {
                throw new IllegalStateException("Unresolved placeholder ${" + name + "} in " + DEFAULT_FILE);
            }
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }
}
