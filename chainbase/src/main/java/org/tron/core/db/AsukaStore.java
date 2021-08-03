package org.tron.core.db;

import org.tron.core.capsule.BytesCapsule;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AsukaStore extends TronStoreWithRevoking<BytesCapsule> {

  private final Map<Integer, BytesCapsule> cache = new ConcurrentHashMap<>();

  protected AsukaStore(String dbName) {
    super(dbName);
  }

  @Override
  public void put(byte[] key, BytesCapsule item) {
    super.put(key, item);
    cache.put(hashCode(key), item);
  }

  @Override
  public BytesCapsule getUnchecked(byte[] key) {
    if (!cache.containsKey(hashCode(key))) {
      cache.put(hashCode(key), getUnchecked(key));
    }
    return cache.get(hashCode(key));
  }

  private int hashCode(byte[] key) {
    return Arrays.hashCode(key);
  }
}
