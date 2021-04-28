# Yaml for Y'all

## Overview
Yaml for Y'all (yyall) is a yaml-based configuration library that uses SnakeYAML to read YAML configurations and provide extra usability on top.

## Motivation
Configurations should be easy to use from code, requiring a minimum of bootstraping to get started. This is accomplished in yyall 
by allowing the user to start from just the factory pattern in `com.github.chrisruffalo.yall.YyallConfiguration` accessed via the `load()` method. 
Using this method allows the user to pass in a path to the configuration file and (optionally) an input stream.

Configurations should be flexible which, in this context, means that a configuration should be easily to modify depending
on the environment, should promote reuse, and should help end-users deal with failure scenarios. In the case of yyall this means accounting
for Java runtime options, environment variables, self-referencing variables, defaults, and even (complex) literals.

## Use
Let's start with a simple example:
```yaml
app:
  api: "http://remote:${app.port}/api"
  port: "9090"
  users:
    - carl
    - lisa
  db:
    type: psql
    host: remote.lan
    port: 3306
    database: app
    url: ${app.db.type}://${app.db.host}:${app.db.port}/${app.db.database}
```

This example allows you to access both individual properties and pre-formatted strings as with the following example:
```java
YyallConfiguration conf = YyallConfiguration.load("/path/to/file.yml");
String port = conf.get("app.db.port");
```

You can also use the same token language to get a more complex resolution:
```java
YyallConfiguration conf = YyallConfiguration.load("/path/to/file.yml");
String complex = conf.format("You can go to the url @ ${app.api}/docs to see the API documents");
```

The language supports more features such as multiple resolution variables, literals, and nested resolution
```java
YyallConfiguration conf = YyallConfiguration.load("/path/to/file.yml");
// resolve opts.api (could be a passed in java option), the yaml value app.api, or use the default localhost value
String multi = conf.format("${ opts.api | app.api | 'http://localhost:9090/api' }");
// same thing but with nested resolution of port value, notice that literals **always** go in quotes
String nested = conf.format("${ opts.api | app.api | 'http://localhost:${app.port | '9090'}/api' }");
```

Since everything in the YAML is a map the normal way to access keys is `map.key` but you can also access indexed
list properties as well.
```java
YyallConfiguration conf = YyallConfiguration.load("/path/to/file.yml");
// resolves as 'lisa' given the above example YAML
String firstUser = conf.format("${ app.users[0] | unknown}"); 
// resolves as 'unknown', index is outside of available users        
String otherUser = conf.format("${ app.users[25] | unknown}"); 
```

You can also use environment variables and system properties, assuming that `-Dapp.format=legacy` was given:
```java
YyallConfiguration conf = YyallConfiguration.load("/path/to/file.yml");
// attempts to resolve app.home, then HOME, then user.home
String home = conf.format("${ app.home | HOME | user.home }");
// resolves app.format (system property) then the literal "modern"
String home = conf.format("${ app.format | 'modern' }");  
```

An entire YAML can be resolved all at once:
```java
YyallConfiguration conf = YyallConfiguration.load("/path/to/file.yml");
Object yaml = conf.resolve();
String yamlString = conf.resovleString();
InputStream yamlStream = conf.resolveStream();
```
