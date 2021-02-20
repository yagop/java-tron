package org.tron.core.db2.core;

import java.util.Map;
import org.tron.core.db2.common.Instance;

public interface Snapshot<U> extends Iterable<Map.Entry<byte[], byte[]>>, Instance<Snapshot<U>> {

  static <U> boolean isRoot(Snapshot<U> snapshot) {
    return snapshot != null && snapshot.getClass() == SnapshotRoot.class;
  }

  static <U> boolean isImpl(Snapshot<U> snapshot) {
    return snapshot != null && snapshot.getClass() == SnapshotImpl.class;
  }

  U get(byte[] key);

  void put(byte[] key, U value);

  void remove(byte[] key);

  void merge(Snapshot<U> from);

  Snapshot<U> advance();

  Snapshot<U> retreat();

  Snapshot<U> getPrevious();

  void setPrevious(Snapshot<U> previous);

  Snapshot<U> getRoot();

  Snapshot<U> getNext();

  void setNext(Snapshot<U> next);

  Snapshot<U> getSolidity();

  void close();

  void reset();

  void resetSolidity();

  void updateSolidity();

  String getDbName();
}
