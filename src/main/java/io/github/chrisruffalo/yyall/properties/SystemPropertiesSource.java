package io.github.chrisruffalo.yyall.properties;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Loads system properties as a property source
 */
public class SystemPropertiesSource implements PropertySource {

    @Override
    public Map<String, String> getProperties() {
        final Map<String, String> outputMap = new HashMap<>();
        System.getProperties().forEach((key, value) -> {
            if (key == null) {
                // no null key
                return;
            }
            final String keyString = String.valueOf(key);
            if (keyString.isEmpty()) {
                // no empty key
                return;
            }
            // but values can be null or empty
            outputMap.put(keyString, value == null ? null : String.valueOf(value));
        });
        return outputMap;
    }
}
