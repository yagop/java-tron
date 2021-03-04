package org.tron.core.db;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.common.reflect.TypeToken;
import com.google.protobuf.GeneratedMessageV3;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.WriteOptions;
import org.rocksdb.DirectComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.storage.rocksdb.RocksDbDataSourceImpl;
import org.tron.common.utils.StorageUtils;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.core.db2.common.DB;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.core.db2.common.LevelDB;
import org.tron.core.db2.common.RocksDB;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.db2.core.ITronChainBase;
import org.tron.core.db2.core.SnapshotRoot;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;


@Slf4j(topic = "DB")
public abstract class TronStoreWithRevoking<T extends ProtoCapsule<U>, U extends GeneratedMessageV3> implements ITronChainBase<T> {

  @Getter // only for unit test
  protected IRevokingDB<U> revokingDB;
  private TypeToken<T> token = new TypeToken<T>(getClass()) {
  };

  private TypeToken<U> subToken = new TypeToken<U>(getClass()) {
  };

  @Autowired
  private RevokingDatabase revokingDatabase;

  protected TronStoreWithRevoking(String dbName) {
    int dbVersion = CommonParameter.getInstance().getStorage().getDbVersion();
    String dbEngine = CommonParameter.getInstance().getStorage().getDbEngine();
    if ("LEVELDB".equals(dbEngine.toUpperCase())) {
      this.revokingDB = new Chainbase<>(new SnapshotRoot<>(
          new LevelDB(
              new LevelDbDataSourceImpl(StorageUtils.getOutputDirectoryByDbName(dbName),
                  dbName,
                  getOptionsByDbNameForLevelDB(dbName),
                  new WriteOptions().sync(CommonParameter.getInstance()
                      .getStorage().isDbSync())))));
    } else if ("ROCKSDB".equals(dbEngine.toUpperCase())) {
      String parentPath = Paths
          .get(StorageUtils.getOutputDirectoryByDbName(dbName), CommonParameter
              .getInstance().getStorage().getDbDirectory()).toString();

      this.revokingDB = new Chainbase<>(new SnapshotRoot<>(
          new RocksDB(
              new RocksDbDataSourceImpl(parentPath,
                  dbName, CommonParameter.getInstance()
                  .getRocksDBCustomSettings(), getDirectComparator()))));
    }

  }

  protected org.iq80.leveldb.Options getOptionsByDbNameForLevelDB(String dbName) {
    return StorageUtils.getOptionsByDbName(dbName);
  }

  protected DirectComparator getDirectComparator() {
    return null;
  }

  protected TronStoreWithRevoking(DB<byte[], byte[]> db) {
    int dbVersion = CommonParameter.getInstance().getStorage().getDbVersion();
    if (dbVersion == 2) {
      this.revokingDB = new Chainbase<U>(new SnapshotRoot<U>(db));
    } else {
      throw new RuntimeException("db version is only 2.(" + dbVersion + ")");
    }
  }

  @Override
  public String getDbName() {
    return null;
  }

  @PostConstruct
  private void init() {
    revokingDatabase.add(revokingDB);
  }

  @Override
  public void put(byte[] key, T item) {
    if (Objects.isNull(key) || Objects.isNull(item)) {
      return;
    }

    revokingDB.put(key, item.getInstance());
  }

  @Override
  public void delete(byte[] key) {
    revokingDB.delete(key);
  }

  @Override
  public T get(byte[] key) throws ItemNotFoundException, BadItemException {
    return of(revokingDB.get(key));
  }

  @Override
  public T getUnchecked(byte[] key) {
    U value = revokingDB.getUnchecked(key);

    try {
      return of(value);
    } catch (BadItemException e) {
      return null;
    }
  }

  public T of(U value) throws BadItemException {
    try {
      Constructor constructor = token.getRawType().getConstructor(subToken.getRawType());
      @SuppressWarnings("unchecked")
      T t = (T) constructor.newInstance((Object) value);
      return t;
    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new BadItemException(e.getMessage());
    }
  }

  @Override
  public boolean has(byte[] key) {
    return revokingDB.has(key);
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  @Override
  public void close() {
    revokingDB.close();
  }

  @Override
  public void reset() {
    revokingDB.reset();
  }

  @Override
  public Iterator<Map.Entry<byte[], T>> iterator() {
    return Iterators.transform(revokingDB.iterator(), e -> {
      try {
        return Maps.immutableEntry(e.getKey(), of(e.getValue()));
      } catch (BadItemException e1) {
        throw new RuntimeException(e1);
      }
    });
  }

  public long size() {
    return Streams.stream(revokingDB.iterator()).count();
  }

  public void setCursor(Chainbase.Cursor cursor) {
    revokingDB.setCursor(cursor);
  }
}
