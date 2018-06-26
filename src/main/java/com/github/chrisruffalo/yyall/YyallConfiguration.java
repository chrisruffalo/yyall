package com.github.chrisruffalo.yyall;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.yaml.snakeyaml.Yaml;

import com.github.chrisruffalo.yyall.resolver.DefaultStringResolver;
import com.github.chrisruffalo.yyall.resolver.StringResolver;

public class YyallConfiguration {

  private static Yaml YAML = new Yaml();
  
  private final Object rootYamlObject;
  
  private final StringResolver resolver;
  
  private final String pathToFile;
    
  private YyallConfiguration(final String pathToFile, final Object rootYamlObject, final StringResolver resolver) {
    this.pathToFile = pathToFile;
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
    
    // if the value is not a type of CharSequence then it cannot be further resolved, so return it
    if(!(value instanceof CharSequence)) {
      return value;
    }
    
    // if the value can be treated as a string, do so
    final String valueString = value.toString();
    final String outputString = this.resolver.resolve(valueString, this.rootYamlObject);        
    
    // return result
    return outputString;
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
  
  public void write() throws IOException {
    YAML.dump(this.rootYamlObject, Files.newBufferedWriter(Paths.get(this.pathToFile)));
  }
  
  public static YyallConfiguration load(final String pathToConfig, final StringResolver resolver) {
    final Path path = Paths.get(pathToConfig);
    final String normalizedPath = path.normalize().toString();
    final Object loaded = YAML.load(path.normalize().toString());
    return new YyallConfiguration(normalizedPath, loaded, resolver);
  }
  
  public static YyallConfiguration load(final String pathToConfig) {
    return load(pathToConfig, new DefaultStringResolver());
  }
  
  public static YyallConfiguration load(final String pathToConfig, final InputStream inputStream, final StringResolver resolver) {
    final Object loaded = YAML.load(inputStream);
    return new YyallConfiguration(pathToConfig, loaded, resolver);
  }
  
  public static YyallConfiguration load(final String pathToConfig, final InputStream inputStream) {
    return load(pathToConfig, inputStream, new DefaultStringResolver());
  }

}
