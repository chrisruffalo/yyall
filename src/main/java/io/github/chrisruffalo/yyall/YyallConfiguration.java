package io.github.chrisruffalo.yyall;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import io.github.chrisruffalo.yyall.bean.PropertyNavigator;
import io.github.chrisruffalo.yyall.properties.EnvironmentVariableSource;
import io.github.chrisruffalo.yyall.properties.PropertySource;
import io.github.chrisruffalo.yyall.properties.SystemPropertiesSource;
import io.github.chrisruffalo.yyall.resolver.DefaultStringResolver;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import io.github.chrisruffalo.yyall.resolver.StringResolver;
import org.yaml.snakeyaml.representer.Representer;

public class YyallConfiguration {

    private static final Yaml YAML = createYaml();

    private final Object rootYamlObject;

    private final StringResolver resolver;

    private final Set<PropertySource> sources = new HashSet<>();

    private boolean useEnvironmentProperties = true;
    private boolean useSystemProperties = true;

    private YyallConfiguration(final Object rootYamlObject, final StringResolver resolver, PropertySource... propertySources) {
        this.rootYamlObject = rootYamlObject;
        this.resolver = resolver;
        if(propertySources != null) {
            this.sources.addAll(Arrays.asList(propertySources));
        }
    }

    public YyallConfiguration withProperties(final PropertySource... propertySources) {
        final YyallConfiguration clone = new YyallConfiguration(this.rootYamlObject, resolver, sources.toArray(new PropertySource[0]));
        if(propertySources != null) {
            clone.sources.addAll(Arrays.asList(propertySources));
        }
        return clone;
    }

    public YyallConfiguration withoutSystemProperties() {
        if (!useSystemProperties) {
            return this;
        }

        final YyallConfiguration clone = new YyallConfiguration(this.rootYamlObject, resolver, sources.toArray(new PropertySource[0]));
        clone.useEnvironmentProperties = this.useEnvironmentProperties;
        clone.useSystemProperties = false;
        return clone;
    }

    public YyallConfiguration withoutEnvironmentVariables() {
        if (!useEnvironmentProperties) {
            return this;
        }

        final YyallConfiguration clone = new YyallConfiguration(this.rootYamlObject, resolver, sources.toArray(new PropertySource[0]));
        clone.useSystemProperties = this.useSystemProperties;
        clone.useEnvironmentProperties = false;
        return clone;
    }

    private Map<String, String> resolvePropertiesFromSources() {
        // don't do anything if sources is empty
        if (sources.isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<String, String> allProperties = new HashMap<>();
        this.sources.forEach(source -> {
            if (source == null) {
                return;
            }
            if (!useEnvironmentProperties && source instanceof EnvironmentVariableSource) {
                return;
            }
            if (!useSystemProperties && source instanceof SystemPropertiesSource) {
                return;
            }
           allProperties.putAll(source.getProperties());
        });
        return allProperties;
    }

    public String get(final String property) {
        return this.resolve(property);
    }

    public String format(final String inputString) {
        return this.format(inputString, Collections.emptyMap());
    }

    @SuppressWarnings("unchecked")
    public String format(final String inputString, Map<String, String> additionalProperties) {
        return this.resolver.resolve(inputString, this.rootYamlObject, this.resolver.defaultProperties(), resolvePropertiesFromSources(), additionalProperties);
    }

    /**
     * Returns a YAML object that has all of the child properties resolved.
     *
     * @return the root yaml object for this instance but with all the properties resolved using the current properties/environment
     */
    public Object resolve() {
        return YAML.load(this.resolveString());
    }

    public <T> T resolveAs(Class<T> targetClass) {
        return YAML.loadAs(this.resolveString(), targetClass);
    }

    public InputStream resolveStream() {
        String resolvedString = this.resolveString();
        if (resolvedString == null) {
            resolvedString = "";
        }
        return new ByteArrayInputStream(resolvedString.getBytes());
    }

    public String resolveString() {
        return this.format(YAML.dump(this.rootYamlObject));
    }

    /**
     * Resolve a single property by repeatedly formatting it until it stabilizes. Relies on format() for the
     * details of the resolution.
     *
     * @param property property value to resolve
     * @return the resolved string, null if not present or not resolvable
     */
    private String resolve(final String property) {
        // no configuration root
        if(this.rootYamlObject == null) {
            return null;
        }

        // can't resolve a property that isn't specified
        if(property == null || property.isEmpty()) {
            return null;
        }

        // attempt to get property from object
        final Object value = PropertyNavigator.getProperty(this.rootYamlObject, property);

        // simple, got a null value... return a null value
        if (value == null) {
            return null;
        }

        // if the value can be treated as a string, do so
        String valueString = null;
        if (value instanceof String) {
            valueString = (String) value;
        } else {
            valueString = value.toString();
        }

        String previous;
        // recursively resolve result
        do {
            previous = valueString;
            valueString = format(valueString);
        } while(!StringUtils.equals(valueString, previous)); // use string utils because either could be null

        return valueString;
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
        return new YyallConfiguration(loaded, resolver, defaultSources());
    }

    public static YyallConfiguration load(final String pathToConfig) {
        return load(pathToConfig, new DefaultStringResolver());
    }

    public static YyallConfiguration load(final InputStream inputStream, final StringResolver resolver) {
        final Object loaded = YAML.load(inputStream);
        return new YyallConfiguration(loaded, resolver, defaultSources());
    }

    public static YyallConfiguration load(final InputStream inputStream) {
        return load(inputStream, new DefaultStringResolver());
    }

    private static PropertySource[] defaultSources() {
        return new PropertySource[]{
            new EnvironmentVariableSource(),
            new SystemPropertiesSource()
        };
    }

    private static Yaml createYaml() {
        final Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(true);

        final DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);

        return new Yaml(representer, dumperOptions);
    }
}
