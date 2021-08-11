package org.tron.common.utils;

import java.nio.ByteBuffer;
import org.rocksdb.ComparatorOptions;
import org.rocksdb.AbstractComparator;
import org.tron.core.capsule.utils.MarketUtils;

public class MarketOrderPriceComparatorForRockDB extends AbstractComparator {

  public MarketOrderPriceComparatorForRockDB(final ComparatorOptions copt) {
    super(copt);
  }

  @Override
  public String name() {
    return "MarketOrderPriceComparator";
  }

  @Override
  public int compare(ByteBuffer a, ByteBuffer b) {
    return MarketUtils.comparePriceKey(convertDataToBytes(a), convertDataToBytes(b));
  }

  /**
   * DirectSlice.data().array will throw UnsupportedOperationException.
   * */
  public byte[] convertDataToBytes(ByteBuffer byteBuffer) {
    int capacity = byteBuffer.capacity();
    byte[] bytes = new byte[capacity];

    for (int i = 0; i < capacity; i++) {
      bytes[i] = byteBuffer.get(i);
    }

    return bytes;
  }

}
