package io.github.chrisruffalo.yyall.properties;

import java.util.Map;

/**
 * A property source is a way of providing properties
 * to the configuration.
 */
public interface PropertySource {

    Map<String, String> getProperties();

}
