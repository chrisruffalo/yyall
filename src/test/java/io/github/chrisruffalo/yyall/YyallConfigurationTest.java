package io.github.chrisruffalo.yyall;

import io.github.chrisruffalo.yyall.model.Root;
import org.junit.Assert;
import org.junit.Test;

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
        final YyallConfiguration conf3 = YyallConfiguration.load(conf.resolveString());

        Assert.assertEquals("Nonsense does not resolve", "no resolution", conf2.get("reference.nonsense"));
        Assert.assertEquals("Format works on non-variable stream", conf.format("${reference.home}"), conf2.format("${reference.home}"));

        Assert.assertEquals("Nonsense does not resolve", "no resolution", conf3.get("reference.nonsense"));
        Assert.assertEquals("Format works on non-variable stream", conf.format("${reference.home}"), conf3.format("${reference.home}"));
    }

    @Test
    public void testResolveNested() {
        final YyallConfiguration conf = YyallConfiguration.load(this.getClass().getResourceAsStream("/featuretest.yml"));
        Assert.assertEquals("Nested literal and variable are resolved correctly", "http://localhost:8080/api", conf.format("${ given.host | 'http://localhost:${vars.port | '9090'}/api'}"));

    }

}
