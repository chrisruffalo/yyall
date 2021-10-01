package io.github.chrisruffalo.yyall.resolver;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;

import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

public class DefaultStringResolverTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testNullResolve() {
        final DefaultStringResolver resolver = new DefaultStringResolver();
        Assert.assertNull("Response should be null", resolver.resolve(null, null));
        Assert.assertNull("Response should be null", resolver.resolve(null, null, (Map<String, String>) null));
    }

    @Test
    public void testEmptyResolve() {
        Assert.assertEquals("", new DefaultStringResolver().resolve("", new Object()));
        Assert.assertEquals("${noprop}", new DefaultStringResolver().resolve("${noprop}", new Object()));
    }

    @Test
    public void testDefault() {
        final DefaultStringResolver resolver = new DefaultStringResolver();
        Assert.assertEquals("No change in string resolution", "no token to resolve", resolver.resolve("no token to resolve"));
        Assert.assertFalse("${user.name} should always resolve to something", resolver.resolve("${user.name}").isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSimpleString() {
        final Map<String, String> testProps = new HashMap<>();
        testProps.put("test", "value");
        final DefaultStringResolver resolver = new DefaultStringResolver();
        final String resolved = resolver.resolve("we are ${test}ing", null, testProps);
        Assert.assertEquals("Test string must match after value is substituted.", "we are valueing", resolved);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSimpleYamlString() {
        final String yaml = "test: value";
        final Object loadedYaml = new Yaml().load(yaml);
        final DefaultStringResolver resolver = new DefaultStringResolver();
        final String resolved = resolver.resolve("we are ${test}ing", loadedYaml, (Map<String, String>) null);
        Assert.assertEquals("Test string must match after value is substituted.", "we are valueing", resolved);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRecursiveResolve() {
        final Map<String, String> testProps = new HashMap<>();
        testProps.put("one", "${two}");
        testProps.put("two", "${three}");
        testProps.put("three", "${four}");
        testProps.put("four", "${five}-${four}");
        testProps.put("five", "done");
        testProps.put("six", "${seven}-${path}");
        testProps.put("seven", "${six}-${path}");
        testProps.put("path", "${seven}-${six}");
        final DefaultStringResolver resolver = new DefaultStringResolver();
        final String resolved = resolver.resolve("${one}-${one} @ ${path}", null, testProps);
        Assert.assertTrue("At least some of the done steps are resolved.", resolved.contains("done"));
        Assert.assertTrue("The ${four} value can never be fully resolved.", resolved.contains("${four}"));
        Assert.assertTrue("The ${path}, ${six}, and ${seven} values can never be fully resolved.", resolved.contains("${six}") && resolved.contains("${path}") && resolved.contains("${seven}"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testYamlDepth() {
        final String yaml = "database: postgres\nvars:\n  status: old\n  state: pending\n  path: ${vars.tmpdir}\n  tmpdir: /home/${database}";
        final Object loadedYaml = new Yaml().load(yaml);
        final DefaultStringResolver resolver = new DefaultStringResolver();
        Assert.assertEquals("Status is old", "old", resolver.resolve("${vars.status}", loadedYaml, (Map<String, String>) null));
        Assert.assertEquals("State is pending", "pending", resolver.resolve("${vars.state}", loadedYaml, (Map<String, String>) null));
        Assert.assertEquals("Path is /home/postgres", "/home/postgres", resolver.resolve("${vars.path}", loadedYaml, (Map<String, String>) null));
    }

    @Test
    public void testAlreadyResolved() {
        final Map<String, String> testProps = new HashMap<>();
        testProps.put("one", "${two}");
        testProps.put("two", "${three}");
        testProps.put("three", "${four}");
        testProps.put("four", "string");
        final DefaultStringResolver resolver = new DefaultStringResolver();
        Assert.assertEquals("Already resolved value is re-used", "string-string-string", resolver.resolve("${one}-${one}-${one}", testProps));
    }

    @Test
    public void testGuardedResolve() {
        final Map<String, String> testProps = new HashMap<>();
        testProps.put("one", "${two}");
        testProps.put("two", "${one}");
        testProps.put("three", "${three}");
        final DefaultStringResolver resolver = new DefaultStringResolver();
        Assert.assertEquals("Guarded property cannot be further resolved", "guarded-${one}", resolver.resolve("guarded-${one}", testProps));
        Assert.assertEquals("Guarded property cannot be further resolved", "guarded-${two}", resolver.resolve("guarded-${two}", testProps));

        // and a self-referential token
        Assert.assertEquals("Token that equals itself cannot be resolved either", "testing-${three}", resolver.resolve("testing-${three}", testProps));
        Assert.assertEquals("Token that equals itself cannot be resolved either", "${three}", resolver.resolve("${three}", testProps));
    }

    @Test
    public void testNestedResolve() {
        final Map<String, String> testProps = new HashMap<>();
        testProps.put("env1", "test");
        testProps.put("env2", "prod");
        testProps.put("layer", "l");
        testProps.put("loc", "section");
        testProps.put("d", "e");
        testProps.put("section1", "sql");
        testProps.put("section2", "db");
        testProps.put("test.sql", "localhost");
        testProps.put("prod.db", "remotehost");
        final DefaultStringResolver resolver = new DefaultStringResolver();
        Assert.assertEquals("Nested test property should be localhost", "localhost", resolver.resolve("${${${d}nv1}.${${${lay${d}r}oc}1}}", testProps));
        Assert.assertEquals("Nested prod property should be remotehost", "remotehost", resolver.resolve("${${${d}nv2}.${${${lay${d}r}oc}2}}", testProps));
    }

    @Test
    public void testAdditionalTokens() {
        final Map<String, String> testProps = new HashMap<>();
        testProps.put("actual", "answer");
        final DefaultStringResolver resolver = new DefaultStringResolver();
        Assert.assertEquals("Resolve property in stage one", "answer", resolver.resolve("${actual}", testProps));
        Assert.assertEquals("Resolve property in stage two", "answer", resolver.resolve("${nothere | actual}", testProps));
        Assert.assertEquals("Resolve property in stage three", "answer", resolver.resolve("${nope | no | actual}", testProps));
    }

    @Test
    public void testLiteralInclusions() {
        final Map<String, String> testProps = new HashMap<>();
        testProps.put("user.home", "/opt/home");
        final DefaultStringResolver resolver = new DefaultStringResolver();
        Assert.assertEquals("Resolve literal in stage one", "literal", resolver.resolve("${ 'literal' }", testProps));
        Assert.assertEquals("Resolve literal in stage two", "literal", resolver.resolve("${ none | \"literal\" }", testProps));
        Assert.assertEquals("Resolve literal in stage three", "literal", resolver.resolve("${ nope | no | 'literal' }", testProps));
        Assert.assertEquals("Resolve literal in stage three with additions", "literal", resolver.resolve("${ nope | no | 'literal' | 'nope'}", testProps));
        Assert.assertEquals("Does not resolve mismatched quotes", "${ 'nope\" }", resolver.resolve("${ 'nope\" }", testProps));
        Assert.assertEquals("Does not resolve wrong quotes", "${ `nope` }", resolver.resolve("${ `nope` }", testProps));
        Assert.assertEquals("Resolves properties in literals", "/env/opt/home", resolver.resolve("${ '/env${user.home}' }", testProps));
    }
}
