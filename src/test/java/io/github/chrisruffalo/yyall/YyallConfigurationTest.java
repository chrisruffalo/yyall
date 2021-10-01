package io.github.chrisruffalo.yyall;

import io.github.chrisruffalo.yyall.model.Root;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class YyallConfigurationTest {

    @Test
    public void testFeaturesYml() {
        final YyallConfiguration conf = YyallConfiguration.load(this.getClass().getResourceAsStream("/featuretest.yml"));
        Assert.assertEquals("Path is correct", "/storage/.storage/", conf.get("app.storage.path"));
    }

    @Test
    public void testFormat() {
        final YyallConfiguration conf = YyallConfiguration.load(this.getClass().getResourceAsStream("/featuretest.yml"));
        Assert.assertEquals("Format works to make string", "storage path '/storage/.storage/' and middlware url 'https://remote.local:8080/api'", conf.format("storage path '${app.storage.path}' and middlware url '${app.middleware.url}'"));
    }

    @Test
    public void testResolveAs() {
        final YyallConfiguration conf = YyallConfiguration.load(this.getClass().getResourceAsStream("/featuretest.yml"));
        final Root root = conf.resolveAs(Root.class);

        Assert.assertEquals("Reference home resolved correctly",  System.getProperty("user.home"), root.getReference().getHome());
        Assert.assertEquals("Nonsense does not resolve", "no resolution", root.getReference().getNonsense());
        Assert.assertEquals("Nested property is resolved", "batman:robin", root.getApp().getStorage().getAuth());
        Assert.assertEquals("Bad property is not resolved", root.getReference().getBad(), "${reference.unknown}");
        Assert.assertEquals("Recursive property is not resolved", root.getReference().getRecursive(), "${reference.recursive}");
        Assert.assertEquals("Second-hand recursive property is not resolved", root.getReference().getSecond(), "${reference.recursive}");

        String javaUser = System.getenv("USER");
        if (javaUser == null || javaUser.isEmpty()) {
            javaUser = System.getProperty("user.name");
        }
        Assert.assertEquals("User is the same", root.getReference().getUser(), javaUser);
    }

    @Test
    public void testResolveTo() {
        final YyallConfiguration conf = YyallConfiguration.load(this.getClass().getResourceAsStream("/featuretest.yml"));
        final InputStream resolvedStream = conf.resolveStream();
        final YyallConfiguration conf2 = YyallConfiguration.load(resolvedStream);
        final YyallConfiguration conf3 = YyallConfiguration.load(new ByteArrayInputStream(conf2.resolveString().getBytes()));

        Assert.assertEquals("Nonsense does not resolve", "no resolution", conf2.get("reference.nonsense"));
        Assert.assertEquals("Format works on non-variable stream", conf.format("${reference.home}"), conf2.format("${reference.home}"));

        Assert.assertEquals("Nonsense does not resolve", "no resolution", conf3.get("reference.nonsense"));
        Assert.assertEquals("Format works on non-variable stream", conf.format("${reference.home}"), conf3.format("${reference.home}"));
    }

    @Test
    public void testResolveNested() {
        final YyallConfiguration conf = YyallConfiguration.load(this.getClass().getResourceAsStream("/featuretest.yml"));
        Assert.assertEquals("Nested literal and variable are resolved correctly", "http://localhost:8080/api", conf.format("${ given.host | 'http://localhost:${vars.port | '9090'}/api'}"));
        Assert.assertEquals("Listed variables are resolved correctly", "robin", conf.format("${ vars.pass }"));
    }

    @Test
    public void testResolveCyclic() {
        final YyallConfiguration conf = YyallConfiguration.load(this.getClass().getResourceAsStream("/featuretest.yml"));
        // cyclic.a resolves to ${cyclic.b} which resolves to cyclic.b which is literally ${cyclic.a} and at that point the
        // engine knows that it is cyclic and stops
        Assert.assertEquals("${cyclic.a}", conf.format("${cyclic.a}"));
        Assert.assertEquals("${cyclic.b}", conf.get("cyclic.a")); // reference is realized to be cyclic at a slightly different step
        Assert.assertEquals("value", conf.format("${cyclic.c}"));
        Assert.assertEquals("value", conf.format("${cyclic.d}"));
        Assert.assertEquals("value", conf.get("cyclic.d"));
    }

    @Test
    public void testRecursiveGet() {
        final YyallConfiguration conf = YyallConfiguration.load(this.getClass().getResourceAsStream("/featuretest.yml"));
        Assert.assertEquals("${reference.recursive}", conf.get("reference.recursive"));
        Assert.assertEquals("${reference.recursive}", conf.get("reference.second"));
    }

    @Test
    public void testDepth() {
        final YyallConfiguration conf = YyallConfiguration.load(this.getClass().getResourceAsStream("/featuretest.yml"));
        Assert.assertEquals("value", conf.get("depth.a"));
        Assert.assertEquals("value", conf.get("depth.b"));
        Assert.assertEquals("value value value", conf.format("${depth.a} ${depth.b} ${depth.a}"));
        Assert.assertEquals("${depth.h[5]}", conf.get("depth.k"));
        // todo: 1.6, make recursion / cyclic bail to the default value
        // Assert.assertEquals("default", conf.format("${ none | depth.k | 'default'}"));
    }

    @Test
    public void testSystemProperties() {
        final YyallConfiguration conf = YyallConfiguration.load(this.getClass().getResourceAsStream("/featuretest.yml"));
        Assert.assertEquals("${custom.system.property}", conf.get("system.value"));
        System.getProperties().put("custom.system.property", "testvalue");
        Assert.assertEquals("testvalue", conf.get("system.value"));
        Assert.assertEquals("${custom.system.property}", conf.withoutSystemProperties().get("system.value"));
    }

    @Test
    public void testEnvironmentProperties() {
        final YyallConfiguration conf = YyallConfiguration.load(this.getClass().getResourceAsStream("/featuretest.yml"));
        // this is not provided on github actions so needs special handling there
        Assert.assertEquals(System.getenv().get("TMP") == null ? "none" : System.getenv().get("TMP"), conf.get("env.tmp"));
        Assert.assertEquals("none", conf.withoutEnvironmentVariables().get("env.tmp"));
    }

    @Test
    public void testMixedMode() {
        final YyallConfiguration conf = YyallConfiguration.load(this.getClass().getResourceAsStream("/featuretest.yml"));
        // on github actions "user.home" is null, thanks github actions
        Assert.assertEquals(System.getProperty("user.home") == null ? "nohome" : System.getProperty("user.home"), conf.get("multi.home"));
        Assert.assertEquals("nohome", conf.withoutEnvironmentVariables().withoutSystemProperties().get("multi.home"));
    }
}
