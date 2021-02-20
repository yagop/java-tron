package org.tron.core.db2.core;

import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.protobuf.GeneratedMessageV3;
import lombok.Getter;
import org.tron.core.db2.common.DB;
import org.tron.core.db2.common.Flusher;
import org.tron.core.db2.common.WrappedByteArray;

public class SnapshotRoot<U extends GeneratedMessageV3> extends AbstractSnapshot<byte[], byte[], U> {

  @Getter
  private Snapshot<U> solidity;

  public SnapshotRoot(DB<byte[], byte[]> db) {
    this.db = db;
    solidity = this;
  }

  @Override
  public U get(byte[] key) {
    return null;
  }

  @Override
  public void put(byte[] key, U value) {
    db.put(key, value.toByteArray());
  }

  @Override
  public void remove(byte[] key) {
    db.remove(key);
  }

  @Override
  public void merge(Snapshot<U> from) {
    SnapshotImpl<U> snapshot = (SnapshotImpl<U>) from;
    Map<WrappedByteArray, WrappedByteArray> batch = Streams.stream(snapshot.db)
        .map(e -> Maps.immutableEntry(WrappedByteArray.of(e.getKey().getBytes()),
            WrappedByteArray.of(e.getValue().getBytes())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    ((Flusher) db).flush(batch);
  }

  public void merge(List<Snapshot<U>> snapshots) {
    Map<WrappedByteArray, WrappedByteArray> batch = new HashMap<>();
    for (Snapshot<U> snapshot : snapshots) {
      SnapshotImpl<U> from = (SnapshotImpl<U>) snapshot;
      Streams.stream(from.db)
          .map(e -> Maps.immutableEntry(WrappedByteArray.of(e.getKey().getBytes()),
              WrappedByteArray.of(e.getValue().getBytes())))
          .forEach(e -> batch.put(e.getKey(), e.getValue()));
    }

    ((Flusher) db).flush(batch);
  }

  @Override
  public Snapshot<U> retreat() {
    return this;
  }

  @Override
  public Snapshot<U> getRoot() {
    return this;
  }

  @Override
  public Iterator<Map.Entry<byte[], byte[]>> iterator() {
    return db.iterator();
  }

  @Override
  public void close() {
    ((Flusher) db).close();
  }

  @Override
  public void reset() {
    ((Flusher) db).reset();
  }

  @Override
  public void resetSolidity() {
    solidity = this;
  }

  @Override
  public void updateSolidity() {
    solidity = solidity.getNext();
  }

  @Override
  public String getDbName() {
    return db.getDbName();
  }

  @Override
  public Snapshot<U> newInstance() {
    return new SnapshotRoot<>(db.newInstance());
  }
}
