package com.github.chrisruffalo.yyall;

import org.junit.Assert;
import org.junit.Test;

public class YyallConfigurationTest {
  
  @Test
  public void testFeaturesYml() {
    final YyallConfiguration conf = YyallConfiguration.load(null, this.getClass().getResourceAsStream("/featuretest.yml"));
    Assert.assertEquals("Path is correct", "/storage", conf.get("app.storage.path"));
  }

  @Test  
  public void testFormat() {
    final YyallConfiguration conf = YyallConfiguration.load(null, this.getClass().getResourceAsStream("/featuretest.yml"));
    Assert.assertEquals("Format works to make string", "storage path '/storage' and middlware url 'https://remote.local:8080/api'", conf.format("storage path '${app.storage.path}' and middlware url '${app.middleware.url}'"));
  }
  
//  @Test
//  public void testWrite() {
//    final YyallConfiguration conf = YyallConfiguration.load(null, this.getClass().getResourceAsStream("/featuretest.yml"));
//    Assert.assertTrue("Mode property is written", conf.put("app.storage,mode", "rw"));
//    Assert.assertEquals("Mode property is rw", "rw", conf.get("app.storage.mode"));
//  }
  
}
