package org.tron.common.utils;

import org.rocksdb.ComparatorOptions;
import org.rocksdb.DirectSlice;
import org.rocksdb.AbstractComparator;
import org.tron.core.capsule.utils.MarketUtils;

import java.nio.ByteBuffer;

public final class MarketOrderPriceComparatorForRockDB extends AbstractComparator {

  public MarketOrderPriceComparatorForRockDB(final ComparatorOptions copt) {
    super(copt);
  }

  @Override
  public String name() {
    return "MarketOrderPriceComparator";
  }

  @Override
  public int compare(final ByteBuffer a, final ByteBuffer b) {
    byte[] byteArrayA = new byte[a.remaining()];
    a.get(byteArrayA);
    byte[] byteArrayB = new byte[a.remaining()];
    b.get(byteArrayB);
    return MarketUtils.comparePriceKey(byteArrayA, byteArrayB);
  }

}
