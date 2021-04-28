package io.github.chrisruffalo.yyall.properties;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Property implements Iterable<Property> {

    private static final String INDEX_START = "[";
    private static final String INDEX_END = "]";
    private static final String MAP_START = "(";
    private static final String MAP_END = ")";

    private static final Pattern INDEX_MATCH = compileMatch(INDEX_START, INDEX_END);
    private static final Pattern MAP_MATCH = compileMatch(MAP_START, MAP_END);

    private Property next;

    private final String segment;

    private Property(final String segment) {
        this.segment = segment;
    }

    public boolean hasNext() {
        return this.next != null;
    }

    public Property next() {
        return this.next;
    }

    public String segment() {
        return this.segment;
    }

    private static Pattern compileMatch(final String start, final String end) {
        final String pattern = String.format("(?:%s)(.+?)(?:%s)", Pattern.quote(start), Pattern.quote(end));
        return Pattern.compile(pattern);
    }

    public static Property parse(final String fulLPropertyPath) {
        if (fulLPropertyPath == null || fulLPropertyPath.isEmpty()) {
            return null;
        }

        final Matcher indexMatcher = INDEX_MATCH.matcher(fulLPropertyPath);
        final Matcher mapMatcher = MAP_MATCH.matcher(fulLPropertyPath);

        // no segments
        if (!fulLPropertyPath.contains(".") && !indexMatcher.find() && !mapMatcher.find()) {
            return new Property(fulLPropertyPath);
        }

        // keep all the segments here so that we can stitch them together later
        final List<Property> properties = new LinkedList<>();

        final String[] segments = StringUtils.split(fulLPropertyPath, ".");
        Arrays.stream(segments).forEach(segment -> {
            Matcher localIndexMatcher = INDEX_MATCH.matcher(segment);

            boolean indexFound = localIndexMatcher.find();

            // if not a map or index segment, return
            if (!indexFound) {
                properties.add(new Property(segment));
                return;
            }

            // while there is a match
            do {
                // find the first match
                int indexMatch = localIndexMatcher.start(0);

                // we need to cut out the first segment
                if (indexMatch != 0) {
                    final String newSegment = segment.substring(0, indexMatch);
                    segment = segment.substring(newSegment.length());
                    properties.add(new Property(newSegment));
                } else {
                    final String indexSegment = segment.substring(indexMatch, localIndexMatcher.end(0));
                    segment = segment.substring(localIndexMatcher.end(0));
                    properties.add(new Property(indexSegment));
                }

                // update match
                localIndexMatcher = INDEX_MATCH.matcher(segment);
                indexFound = localIndexMatcher.find();
            } while (indexFound);

            // if there is a remaining segment discard it (possibly warn?)
        });

        // go through the list of properties and set up the property chain output
        Property property = null;
        Property previous = null;
        for (Property prop : properties) {
            // set root property
            if (property == null) {
                property = prop;
            }
            if (previous != null) {
                previous.next = prop;
            }
            previous = prop;
        }

        // return root
        return property;
    }

    @Override
    public Iterator<Property> iterator() {
        final Property nullProperty = new Property(null);
        nullProperty.next = this;
        return new Iterator<Property>() {
            private Property current = nullProperty;

            @Override
            public boolean hasNext() {
                return current.hasNext();
            }

            @Override
            public Property next() {
                if(hasNext()) {
                    current = current.next();
                    return current;
                }
                return null;
            }
        };
    }

    @Override
    public void forEach(Consumer<? super Property> action) {
        this.iterator().forEachRemaining(action);
    }

    @Override
    public Spliterator<Property> spliterator() {
        return Iterable.super.spliterator();
    }
}
