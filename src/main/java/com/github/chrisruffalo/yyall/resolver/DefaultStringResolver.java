package com.github.chrisruffalo.yyall.resolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class DefaultStringResolver implements StringResolver {
  
  private final static String DEFAULT_START_TOKEN = "${";
  private final static String DEFAULT_END_TOKEN = "}";
  private final static String DEFAULT_PIPE_TOKEN = "|";
  
  private String startToken = DEFAULT_START_TOKEN;
  private String endToken = DEFAULT_END_TOKEN;
  private String pipeToken = DEFAULT_PIPE_TOKEN;
  
  private Set<String> literalQuoteSet = new HashSet<>();
  
  private Pattern tokenPattern = null;
  
  public DefaultStringResolver() {
    this.literalQuoteSet.add("'");
    this.literalQuoteSet.add("\"");
    this.recompileMatch();
  }
  
  public void setStartToken(final String newStartToken) {
    this.startToken = newStartToken;
    this.recompileMatch();
  }
  
  public void setEndToken(final String newEndToken) {
    this.endToken = newEndToken;
    this.recompileMatch();
  }
  
  private void recompileMatch() {
    // creates a non-greedy match for a token that is between the given token strings but that does not contain the token itself
    // so that things like ${${env}-${section}} can resolve through the recursive resolution process
    final String pattern = String.format("(?:%s)([^%s]+?)(?:%s)", Pattern.quote(this.startToken), Pattern.quote(this.startToken + this.endToken), Pattern.quote(this.endToken));
    this.tokenPattern = Pattern.compile(pattern);
  }
  
  private List<String> findMatchingTokens(final String input) {
    final List<String> matches = new ArrayList<>();
    
    final Matcher matcher = this.tokenPattern.matcher(input);
    while(matcher.find()) {
      matches.add(matcher.group(1));
    }
    
    return Collections.unmodifiableList(matches);
  }
  
  private Map<String, String> propertiesToMap(final Properties properties) {
        return properties.entrySet().stream().collect(
            Collectors.toMap(
                e -> e.getKey().toString(),
                e -> e.getValue().toString()
            )
        );
    }

    public Map<String, String> defaultProperties() {
        final Map<String, String> defaultMap = new HashMap<>(System.getProperties().size() + System.getenv().size());
        defaultMap.putAll(System.getenv());
        defaultMap.putAll(this.propertiesToMap(System.getProperties()));
        return defaultMap;
    }

    @SuppressWarnings("unchecked")
    public String resolve(final String inputString) {
        return this.resolve(inputString, null, this.defaultProperties());
    }

    @SuppressWarnings("unchecked")
    public String resolve(final String inputString, final Object yaml) {
        return this.resolve(inputString, yaml, this.defaultProperties());
    }

    @SuppressWarnings("unchecked")
    public String resolve(final String inputString, final Map<String, String> properties) {
        return this.resolve(inputString, null, properties);
    }

    @SuppressWarnings("unchecked")
    public String resolve(final String inputString, final Object yaml, final Map<String, String>... propertyMaps) {
        // merge property map input
        final Map<String, String> properties = new HashMap<>();
        if(propertyMaps != null && propertyMaps.length > 0) {
            ArrayUtils.reverse(propertyMaps);
            Arrays.stream(propertyMaps).forEach(map -> {
                if(map != null && !map.isEmpty()) {
                    properties.putAll(map);
                }
            });
        }

        // store previous values in a hash set so that we can
        // do resolution in a way that prevents cyclic resolution
        // (so that we can't get stuck resolving and re-resolving the same properties)
        final Set<String> previousValues = new HashSet<>();

        return this.resolve("", new HashSet<String>(), previousValues, inputString, yaml, properties);
    }

    private String resolve(final String prefix, final Set<String> guardPropertySet, final Set<String> previousValues, final String inputString, final Object yaml, final Map<String, String> properties) {
        // ==============
        // i took this (almost) wholesale from ee-config, another of my projects
        // https://github.com/chrisruffalo/ee-config/blob/master/src/main/java/com/github/chrisruffalo/eeconfig/strategy/property/DefaultPropertyResolver.java
        // ==============

        // return same string if it is null or empty
        if(inputString == null || inputString.isEmpty()) {
            return inputString;
        }

        // working string copy
        String workingString = inputString;

        // can't do contains check here because working string is contained right at the start
        // can only do it after at least some potential transform steps
        do {
            // add working string updates to previous values
            previousValues.add(workingString);

            // resolve special tokens
            final List<String> foundTokens = this.findMatchingTokens(workingString);

            for(final String token : foundTokens) {
                if(token == null || token.isEmpty() || guardPropertySet.contains(token)) {
                    continue;
                }
                
                // split token on pipe
                final String[] tokenParts = StringUtils.split(token, this.pipeToken);
                
                for(String currentToken : tokenParts) {
                  if(currentToken == null || currentToken.isEmpty() || guardPropertySet.contains(token)) {
                    continue;
                  }
                  // trim token
                  currentToken = currentToken.trim();
                  
                  // start with null property string                
                  String property = null;
                  
                  // if the token is a literal then we should use that as the property
                  if(currentToken.charAt(0) == currentToken.charAt(currentToken.length() - 1) && this.literalQuoteSet.contains(String.valueOf(currentToken.charAt(0)))) {
                    final String literal = currentToken.substring(1, currentToken.length() - 1);
                    property = this.resolve(literal, properties);
                  } 
 
                  // next, if the property is still null, try and read yaml
                  // (nested maps - which the yaml object is - can be read with BeanUtils)
                  if(property == null && yaml != null) {
                      try {
                          Object found = BeanUtils.getProperty(yaml, currentToken);
                          if(found != null) {
                              property = found.toString();
                          }
                      } catch (Exception e) {
                          // no-op
                      }
                  }
  
                  // if the property is still null look it up
                  if(property == null) {
                      property = properties.get(currentToken);
                  }
  
                  // skip null properties
                  if(property != null) {
                    // get prefix
                    String foundPrefix = "";
                    if(currentToken.lastIndexOf(".") >= 1) {
                      foundPrefix = currentToken.substring(0);
                    }
                    
                    // resolve property before replacing
                    final HashSet<String> resolveGuardSet = new HashSet<>();
                    resolveGuardSet.addAll(guardPropertySet);
                    resolveGuardSet.add(currentToken);
                    final String resolvedProperty = this.resolve(foundPrefix, resolveGuardSet, previousValues, property, yaml, properties);
  
                    // do actual replacement against _WHOLE TOKEN_
                    workingString = StringUtils.replace(workingString, this.startToken + token + this.endToken, resolvedProperty);
                    
                    // done
                    break;
                  }
                } // done with token subpart
            } // done with token
            
            // guard against all found tokens
            guardPropertySet.addAll(foundTokens);

        } while (!previousValues.contains(workingString));

        return workingString;
    }
 
}
