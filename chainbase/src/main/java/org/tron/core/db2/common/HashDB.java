package org.tron.core.db2.common;

import com.google.protobuf.GeneratedMessageV3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HashDB<U extends GeneratedMessageV3> implements DB<Key, InstanceValue<U>> {

  private Map<Key, InstanceValue<U>> db = new ConcurrentHashMap<>();
  private String name;

  public HashDB(String name) {
    this.name = name;
  }

  @Override
  public InstanceValue<U> get(Key key) {
    return db.get(key);
  }

  @Override
  public void put(Key key, InstanceValue<U> value) {
    db.put(key, value);
  }

  @Override
  public long size() {
    return db.size();
  }

  @Override
  public boolean isEmpty() {
    return db.isEmpty();
  }

  @Override
  public void remove(Key key) {
    db.remove(key);
  }

  @Override
  public String getDbName() {
    return name;
  }

  @Override
  public Iterator<Map.Entry<Key, InstanceValue<U>>> iterator() {
    return db.entrySet().iterator();
  }

  @Override
  public void close() {
    db.clear();
  }

  @Override
  public HashDB<U> newInstance() {
    return new HashDB<>(name);
  }
}
