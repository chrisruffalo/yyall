package io.github.chrisruffalo.yyall.properties;

import org.junit.Assert;
import org.junit.Test;

public class PropertyTest {

    @Test
    public void testEmptyAndNull() {
        Assert.assertNull("An empty property produces a null property chain", Property.parse(""));
        Assert.assertNull("A null property produces a null property chain", Property.parse(null));
    }

    @Test
    public void testSimpleProperty() {
        final Property simple = Property.parse("simpleProperty");
        Assert.assertNotNull(simple);
        Assert.assertEquals("Simple property segment is correct", "simpleProperty", simple.segment());
        Assert.assertFalse("Simple property has no next", simple.hasNext());
        Assert.assertNull("Simple property next is null", simple.next());
    }

    @Test
    public void testMultiSegmentProperty() {
        final Property segmented = Property.parse("segment.segment2.segment3");
        Assert.assertNotNull(segmented);
        Assert.assertEquals("First segment is 'segment'", "segment", segmented.segment());
        Assert.assertTrue("Segment has next", segmented.hasNext());
        Assert.assertEquals("Second segment is 'segment2'", "segment2", segmented.next().segment());
        Assert.assertTrue("Segment.segment2 has next", segmented.next().hasNext());
        Assert.assertEquals("Third segment is 'segment3'", "segment3", segmented.next().next().segment());
        Assert.assertFalse("Final segment has non more", segmented.next().next().hasNext());
    }

    @Test
    public void testIndexSegment() {
        final Property indexed = Property.parse("first.second[1231]");
        Assert.assertTrue("First has second", indexed.hasNext());
        Assert.assertEquals("Value of segment is 'second'", "second", indexed.next().segment());
        Assert.assertTrue("Second has index segment", indexed.next().hasNext());
        Property index = indexed.next().next();
        Assert.assertEquals("Indexed segment is '[1231]'", "[1231]", index.segment());

        Property both = Property.parse("first.second[1234][5678]");
        Assert.assertNotNull("Third property is available", both.next().next());
        Assert.assertNotNull("Fourth property is available", both.next().next().next());
    }

    @Test
    public void testComplex() {
        Property complex = Property.parse("first.second[1234][5678].key.third.fourth[123].nonsense[123]");
        final String[] segments = {
            "first",
            "second",
            "[1234]",
            "[5678]",
            "key",
            "third",
            "fourth",
            "[123]",
            "nonsense",
            "[123]"
        };

        int idx = 0;
        for (Property property : complex) {
            Assert.assertEquals(String.format("The segment index %d should equal %s", idx, segments[idx]), segments[idx], property.segment());
            idx++;
        }
    }

}
