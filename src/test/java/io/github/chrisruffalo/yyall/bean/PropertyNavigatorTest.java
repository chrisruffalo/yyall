package io.github.chrisruffalo.yyall.bean;

import org.junit.Assert;
import org.junit.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class PropertyNavigatorTest {

    private static Object load(final String resourcePath) {
        final DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);
        final Yaml yaml = new Yaml(dumperOptions);

        return yaml.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath));
    }

    @Test
    public void testNavigation() {
        final Object source = load("featuretest.yml");
        Assert.assertNotNull(source);

        // create property path
        final Property property = Property.parse("vars.storage");

        // look up simple property
        final Object storageObject = PropertyNavigator.getProperty(source, property);
        Assert.assertNotNull("Storage object should be found", storageObject);
        Assert.assertEquals("Storage object equals '/storage'", "/storage", storageObject.toString());

        // look up complex property
        final Property complex = Property.parse("vars.list[6][1]");
        final Object nestedObject = PropertyNavigator.getProperty(source, complex);
        Assert.assertNotNull("Nested object should be found and not null", nestedObject);
        Assert.assertEquals("Nested object should be 'two'", "two", nestedObject.toString());
    }

}
