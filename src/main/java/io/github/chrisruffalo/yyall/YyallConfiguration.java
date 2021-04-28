package io.github.chrisruffalo.yyall;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.chrisruffalo.yyall.properties.PropertyNavigator;
import io.github.chrisruffalo.yyall.resolver.DefaultStringResolver;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import io.github.chrisruffalo.yyall.resolver.StringResolver;
import org.yaml.snakeyaml.constructor.BaseConstructor;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

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
        final Object value = PropertyNavigator.getProperty(this.rootYamlObject, property);

        // simple, got a null value... return a null value
        if(value == null) {
            return null;
        }

        // if the value can be treated as a string, do so
        String valueString = null;
        if (value instanceof String) {
            valueString = (String)value;
        } else {
            valueString = value.toString();
        }

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
        final Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(true);

        final DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);

        return new Yaml(representer, dumperOptions);
    }
}
