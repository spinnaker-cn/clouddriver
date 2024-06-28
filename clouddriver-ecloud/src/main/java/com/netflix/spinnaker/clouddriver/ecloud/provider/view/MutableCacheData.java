package com.netflix.spinnaker.clouddriver.ecloud.provider.view;

import com.netflix.spinnaker.cats.cache.CacheData;
import java.util.*;

public class MutableCacheData implements CacheData {
  private final String id;
  private int ttlSeconds = -1;
  private Map<String, Object> attributes = new HashMap<>();
  private Map<String, Collection<String>> relationships = new HashMap<>();

  public MutableCacheData(String id) {
    this.id = id;
  }

  @Override
  public String getId() {
    return id;
  }

  public int getTtlSeconds() {
    return ttlSeconds;
  }

  public void setTtlSeconds(int ttlSeconds) {
    this.ttlSeconds = ttlSeconds;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  public Map<String, Collection<String>> getRelationships() {
    return relationships;
  }

  public void setRelationships(Map<String, Collection<String>> relationships) {
    this.relationships = relationships;
  }
}
