# Yaml for Y'all

## Overview
Yaml for Y'all (yyall) is a yaml-based configuration library that uses SnakeYAML to read YAML configurations and provide extra usability on top.

## Motivation
I believe that configurations should be easy to use from code, requiring a minimum of bootstraping to get started. This is accomplished in yyall 
by allowing the user to start from just the factory pattern in `com.github.chrisruffalo.yall.YyallConfiguration` accessed via the `load()` method. 
Using this method allows the user to pass in a path to the configuration file and (optinally) an input stream.

Configurations should be flexible which, I'll admit, is hard to define. In this context I mean that a configuration should be easily to modify depending
on the environment, should promote reuse, and should help end-users deal with failure scenarios. In the case of yyall this means that we can account
for Java runtime options, self-referencing variables, defaults, and even (complex) literals. On their own many of these features are useless but 
by using these in concert we can create robust configuration files that are easy to work with.

Configurations should also not be strictly bound to a target model. While those are useful for validation and for structured data access they can hamper
the use of advanced features and prevent users from creating extended configuration sets.

## Use
Let's start with a simple example:
```yaml
app:
  api: "http://remote:${app.port}/api"
  port: "9090"
  db:
    type: psql
    host: remote.lan
    port: 3306
    database: app
    url: ${app.db.type}://${app.db.host}:${app.db.port}/${app.db.database}
```

This example allows you to access both individual properties and pre-formatted strings as with the following example:
```java
final YyallConfiguration conf = YyallConfiguration.load("/path/to/file.yml");
final String port = conf.get("app.db.port");
```

You can also use the same token language to get a more complex resolution:
```java
final YyallConfiguration conf = YyallConfiguration.load("/path/to/file.yml");
final String complex = conf.format("You can go to the url @ ${app.api}/docs to see the API documents")
```
The language supports more features such as multiple resolution variables, literals, and nested resolution
```java
final YyallConfiguration conf = YyallConfiguration.load("/path/to/file.yml");
// resolve opts.api (could be a passed in java option), the yaml value app.api, or use the default localhost value
final String multi = conf.get("${ opts.api | app.api | 'http://localhost:9090/api' }")
// same thing but with nested resolution of port value, notice that literals **always** go in quotes
final String multi = conf.get("${ opts.api | app.api | 'http://localhost:${app.port | '9090'}/api' }")
```
