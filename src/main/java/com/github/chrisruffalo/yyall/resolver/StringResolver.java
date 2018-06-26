package com.github.chrisruffalo.yyall.resolver;

import java.util.Map;

/**
 * A string resolver takes a string and uses an object or property map to resolve that property. At
 * its core the purpose is to resolve strings like "${java.home}" into "/home/user" and to do so
 * recursively and to allow properties and configuration to work together on hosts so that broader
 * and more fluent configurations can be created by application specialists and shaped to each
 * deployment target through the use of supplemental variables or other tokens at runtime.
 * 
 */
public interface StringResolver {

    String resolve(final String inputString);

    String resolve(final String inputString, final Object yaml);

    String resolve(final String inputString, final Map<String, String> properties);

    @SuppressWarnings("unchecked")
    String resolve(final String inputString, final Object yaml, final Map<String, String>... propertyMaps);
    
    Map<String, String> defaultProperties();

}
