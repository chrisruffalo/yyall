package io.github.chrisruffalo.yyall.properties;

import org.apache.commons.beanutils.PropertyUtils;

import java.lang.reflect.InvocationTargetException;

public class PropertyNavigator {

    public static Object getProperty(final Object object, final String property) {
        return getProperty(object, Property.parse(property));
    }

    public static Object getProperty(final Object object, final Property property) {
        Property current = property;
        Object gotten = null;
        while(current != null) {
            try {
                gotten = PropertyUtils.getProperty(gotten == null ? object : gotten, current.segment());
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                // can't do anything about this and can't go further
                return null;
            }
            if (gotten == null) {
                break;
            }
            current = current.next();
        }
        return gotten;
    }

}
