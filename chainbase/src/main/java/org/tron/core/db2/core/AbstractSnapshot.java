package org.tron.core.db2.core;

import java.lang.ref.WeakReference;

import com.google.protobuf.GeneratedMessageV3;
import lombok.Getter;
import lombok.Setter;
import org.tron.core.db2.common.DB;

public abstract class AbstractSnapshot<K, V, U extends GeneratedMessageV3> implements Snapshot<U> {

  @Getter
  protected DB<K, V> db;
  @Getter
  @Setter
  protected Snapshot<U> previous;

  protected WeakReference<Snapshot<U>> next;

  @Override
  public Snapshot<U> advance() {
    return new SnapshotImpl<>(this);
  }

  @Override
  public Snapshot<U> getNext() {
    return next == null ? null : next.get();
  }

  @Override
  public void setNext(Snapshot<U> next) {
    this.next = new WeakReference<>(next);
  }

  @Override
  public String getDbName() {
    return db.getDbName();
  }
}
