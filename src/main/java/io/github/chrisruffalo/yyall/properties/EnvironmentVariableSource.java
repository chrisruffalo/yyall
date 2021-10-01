package io.github.chrisruffalo.yyall.properties;

import java.util.Map;

/**
 * Adds environment variables as a property source
 */
public class EnvironmentVariableSource implements PropertySource {

    @Override
    public Map<String, String> getProperties() {
        return System.getenv();
    }
}
