package com.github.chrisruffalo.yyall;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import com.github.chrisruffalo.yyall.resolver.DefaultStringResolver;
import com.github.chrisruffalo.yyall.resolver.StringResolver;

public class YyallConfiguration {

    private static final Yaml YAML = createYaml();

    private final Object rootYamlObject;

    private final StringResolver resolver;

    private YyallConfiguration(final Object rootYamlObject, final StringResolver resolver) {
        this.rootYamlObject = rootYamlObject;
        this.resolver = resolver;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(final String property) {
        return (T)this.resolve(property);
    }

    public String format(final String inputString) {
        return this.format(inputString, Collections.emptyMap());
    }

    @SuppressWarnings("unchecked")
    public String format(final String inputString, Map<String, String> additionalProperties) {
        return this.resolver.resolve(inputString, this.rootYamlObject, this.resolver.defaultProperties(), additionalProperties);
    }

    private void getAllProperties(final Object yaml, final String prefix, final Map<Object, Object> propertiesCollector, final StringResolver resolver) {
        if (yaml == null) {
            return;
        }

        if (yaml instanceof Iterable) {
            for(final Object object : (Iterable<?>) yaml) {
                getAllProperties(object, "", propertiesCollector, resolver);
            }
        }

        if (yaml instanceof Map) {
            for(final Map.Entry<?, ?> entry : ((Map<?,?>) yaml).entrySet()) {
                final Object key = entry.getKey();
                if ( key == null ) {
                    continue;
                }
                final String keyString = (key instanceof String) ? (String)key : key.toString();
                final Object value = entry.getValue();
                if (value instanceof Map) {
                    final Map<Object, Object> subMap = new LinkedHashMap<>();
                    propertiesCollector.put(keyString, subMap);
                    this.getAllProperties(value, String.format("%s%s.", prefix, keyString), subMap, resolver);
                } else {
                    final String propertyKey = String.format("%s%s", prefix, keyString);
                    propertiesCollector.put(key, this.get(propertyKey));
                }
            }
        }
    }

    /**
     * Returns a YAML object that has all of the child properties resolved.
     *
     * @return the root yaml object for this instance but with all the properties resolved using the current properties/environment
     */
    public Object resolve() {
        final Map<Object, Object> properties = new LinkedHashMap<>();
        this.getAllProperties(rootYamlObject, "", properties, resolver);
        return properties;
    }

    public <T> T resolveAs(Class<T> targetClass) {
        final Object resolved = this.resolve();
        return YAML.loadAs(YAML.dump(resolved), targetClass);
    }

    public InputStream resolveStream() {
        String resolvedString = YAML.dump(this.resolve());
        if (resolvedString == null) {
            resolvedString = "";
        }
        return new ByteArrayInputStream(resolvedString.getBytes());
    }

    public String resolveString() {
        return YAML.dump(this.resolve());
    }

    private Object resolve(final String property) {
        final HashSet<String> guardSet = new HashSet<>();
        guardSet.add(property);
        return this.resolve("", property, guardSet, new HashSet<>(), new HashMap<>());
    }

    private Object resolve(final String base, final String property, final Set<String> guard, final Set<String> cyclic, final Map<String, String> alreadyResolved) {
        // no configuration root
        if(this.rootYamlObject == null) {
            return null;
        }

        // can't resolve a property that isn't specified
        if(property == null || property.isEmpty()) {
            return null;
        }

        // attempt to get property from object
        Object value = null;
        try {
            value = BeanUtils.getProperty(this.rootYamlObject, property);
        } catch (final Exception e) {
            // no value found, return null
            return null;
        }

        // simple, got a null value... return a null value
        if(value == null) {
            return null;
        }

        // if the value can be treated as a string, do so
        final String valueString = value.toString();

        // return result
        return this.resolver.resolve(valueString, this.rootYamlObject);
    }

    public boolean put(final String key, final Object value) {
        try {
            // try and set property on yaml object
            BeanUtils.setProperty(this.rootYamlObject, key, value);
            return true;
        } catch(Exception ex) {
            // no-op on no set
        }
        return false;
    }

    public static YyallConfiguration load(final String pathToConfig, final StringResolver resolver) {
        final Path path = Paths.get(pathToConfig).normalize().toAbsolutePath();
        final Object loaded = YAML.load(path.normalize().toString());
        return new YyallConfiguration(loaded, resolver);
    }

    public static YyallConfiguration load(final String pathToConfig) {
        return load(pathToConfig, new DefaultStringResolver());
    }

    public static YyallConfiguration load(final InputStream inputStream, final StringResolver resolver) {
        final Object loaded = YAML.load(inputStream);
        return new YyallConfiguration(loaded, resolver);
    }

    public static YyallConfiguration load(final InputStream inputStream) {
        return load(inputStream, new DefaultStringResolver());
    }

    private static Yaml createYaml() {
        final DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);
        return new Yaml(dumperOptions);
    }
}
