package org.tron.core.store;

import com.typesafe.config.ConfigObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.BlockQueueFactoryUtil;
import org.tron.common.utils.Commons;
import org.tron.common.utils.ThreadPoolUtil;
import org.tron.common.utils.TimerUtil;
import org.tron.core.capsule.AccountAssetIssueCapsule;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.AccountAssetIssue;

@Slf4j(topic = "DB")
@Component
public class AccountAssetIssueStore extends TronStoreWithRevoking<AccountAssetIssueCapsule> {

  private static Map<String, byte[]> assertsAddress = new HashMap<>();
  @Autowired
  @Setter
  private AccountStore accountStore;
    // key = name , value = address
  private AtomicBoolean readFinish = new AtomicBoolean(false);
  private AtomicLong readCount = new AtomicLong(0);
  private AtomicLong readCost = new AtomicLong(0);
  private AtomicLong writeCount = new AtomicLong(0);
  private AtomicLong writeCost = new AtomicLong(0);

  @Autowired
  public AccountAssetIssueStore(@Value("account-asset-issue") String dbName) {
    super(dbName);
  }

  public static void setAccountAssetIssue(com.typesafe.config.Config config) {
    List list = config.getObjectList("genesis.block.assets");
    for (int i = 0; i < list.size(); i++) {
      ConfigObject obj = (ConfigObject) list.get(i);
      String accountName = obj.get("accountName").unwrapped().toString();
      byte[] address = Commons.decodeFromBase58Check(obj.get("address").unwrapped().toString());
      assertsAddress.put(accountName, address);
    }
  }

  @Override
  public AccountAssetIssueCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new AccountAssetIssueCapsule(value);
  }

  /**
   * Min TRX account.
   */
  public AccountAssetIssueCapsule getBlackhole() {
    return getUnchecked(assertsAddress.get("Blackhole"));
  }

  public void RollbackAssetIssueToAccount() {
    long start = System.currentTimeMillis();
    logger.info("rollback asset to account store");
    CountDownLatch countDownLatch = new CountDownLatch(1);
    Timer timer = null;

    AccountAssetIssueRecordQueue accountRecordQueue = new AccountAssetIssueRecordQueue(
      BlockQueueFactoryUtil.getInstance(),
      countDownLatch);
    accountRecordQueue.fetchAccountAssetIssue(this.getRevokingDB());
    AccountConvertQueue accountConvertQueue = new AccountConvertQueue(
      BlockQueueFactoryUtil.getInstance(),
      this,
      accountStore);
    accountConvertQueue.convertAccountAssetIssueToAccount();

    try {
      timer = TimerUtil.countDown("rollback account asset issue to account time spent ", readCount, writeCount);
      countDownLatch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      TimerUtil.cancel(timer);
    }
    long readC = readCount.get();
    long writeC = writeCount.get();
    logger.info("import asset time: {}s, r({}) - w({}) = diff({})",
        (System.currentTimeMillis() - start) / 1000,
        readC,
        writeC,
        readC - writeC);
  }

  public void convertAccountAssert() {
    long start = System.currentTimeMillis();
    logger.info("import asset of account store to account asset store ");
    CountDownLatch countDownLatch = new CountDownLatch(1);
    Timer timer =
        TimerUtil.countDown("import asset time spent ", readCount, writeCount);
    try {
      AccountAssetIssueRecordQueue accountRecordQueue = new AccountAssetIssueRecordQueue(
          BlockQueueFactoryUtil.getInstance(),
          countDownLatch);
      accountRecordQueue.fetchAccount(accountStore.getRevokingDB());
      AccountConvertQueue accountConvertQueue = new AccountConvertQueue(
          BlockQueueFactoryUtil.getInstance(),
          this,
          accountStore);
      try {
        accountConvertQueue.convert();
      } catch (ExecutionException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    } finally {
      TimerUtil.cancel(timer);
    }

    long readC = readCount.get();
    long writeC = writeCount.get();
    logger.info("import asset time: {}s, r/w {}/{}, r({}) - w({}) = diff({})",
        (System.currentTimeMillis() - start) / 1000,
        readCost.get() / 1000,
        writeCost.get() / 1000,
        readC,
        writeC,
        readC - writeC);
  }

  public class AccountAssetIssueRecordQueue {

    private final CountDownLatch countDownLatch;

    private BlockingQueue productQueue;

    public AccountAssetIssueRecordQueue(BlockingQueue<Map.Entry<byte[], byte[]>> productQueue,
                                        CountDownLatch countDownLatch) {
      this.productQueue = productQueue;
      this.countDownLatch = countDownLatch;
    }

    public void put(Map.Entry<byte[], byte[]> accountByte) {
      try {
        productQueue.put(accountByte);
      } catch (InterruptedException e) {
        logger.error("put account asset issue exception: {}", e.getMessage(), e);
        Thread.currentThread().interrupt();
      }
    }

    public void fetchAccount(IRevokingDB revokingDB) {
      fetch(revokingDB);
    }

    public void fetchAccountAssetIssue(IRevokingDB revokingDB) {
      fetch(revokingDB);
    }

    private void fetch(IRevokingDB revokingDB) {
      Executors.newSingleThreadExecutor().execute(() -> {
        long start = System.currentTimeMillis();
        for (Map.Entry<byte[], byte[]> accountRecord : revokingDB) {
          put(accountRecord);
          readCount.incrementAndGet();
        }
        readFinish.set(true);
        readCost.set(System.currentTimeMillis() - start);
//                countDownLatch.countDown();
      });
    }
  }

  public class AccountConvertQueue {

    private BlockingQueue<Map.Entry<byte[], byte[]>> convertQueue;

    private AccountAssetIssueStore accountAssetIssueStore;

    private AccountStore accountStore;

    List<Future<?>> writeFutures = new ArrayList<>();

    public AccountConvertQueue(BlockingQueue<Map.Entry<byte[], byte[]>> convertQueue,
                               AccountAssetIssueStore accountAssetIssueStore) {
      this.convertQueue = convertQueue;
      this.accountAssetIssueStore = accountAssetIssueStore;
    }

    public AccountConvertQueue(BlockingQueue<Map.Entry<byte[], byte[]>> convertQueue,
                               AccountAssetIssueStore accountAssetIssueStore,
                               AccountStore accountStore) {
      this.convertQueue = convertQueue;
      this.accountAssetIssueStore = accountAssetIssueStore;
      this.accountStore = accountStore;
    }

    public void convert() throws ExecutionException, InterruptedException {
      long start = System.currentTimeMillis();
      ExecutorService writeExecutor = Executors.newFixedThreadPool(ThreadPoolUtil.getMaxPoolSize());
      for (int i = 0; i < ThreadPoolUtil.getMaxPoolSize(); i++) {
        Future<?> future = writeExecutor.submit(() -> {
          try {
            while (true) {
              Map.Entry<byte[], byte[]> accountEntry = convertQueue.poll();
              if (readFinish.get() && accountEntry == null) {
                break;
              }

              if (accountEntry == null) {
                TimeUnit.MILLISECONDS.sleep(5);
                continue;
              }

              AccountCapsule accountCapsule =
                new AccountCapsule(accountEntry.getValue());
              byte[] address = accountCapsule.getAddress().toByteArray();
              AccountAssetIssue accountAssetIssue = AccountAssetIssue.newBuilder()
                .setAddress(accountCapsule.getAddress())
                .setAssetIssuedID(accountCapsule.getAssetIssuedID())
                .setAssetIssuedName(accountCapsule.getAssetIssuedName())
                .putAllAsset(accountCapsule.getAssetMap())
                .putAllAssetV2(accountCapsule.getAssetMapV2())
                .putAllFreeAssetNetUsage(accountCapsule.getAllFreeAssetNetUsage())
                .putAllFreeAssetNetUsageV2(
                  accountCapsule.getAllFreeAssetNetUsageV2())
                .putAllLatestAssetOperationTime(
                  accountCapsule.getLatestAssetOperationTimeMap())
                .putAllLatestAssetOperationTimeV2(
                  accountCapsule.getLatestAssetOperationTimeMapV2())
                .build();

              accountAssetIssueStore.put(address,
                new AccountAssetIssueCapsule(accountAssetIssue));
              Account account = accountCapsule.getInstance();
              account = account.toBuilder()
                  .clearAssetIssuedID()
                  .clearAssetIssuedName()
                  .clearAsset()
                  .clearAssetV2()
                  .clearFreeAssetNetUsage()
                  .clearFreeAssetNetUsageV2()
                  .clearLatestAssetOperationTime()
                  .clearLatestAssetOperationTimeV2()
                  .build();
              accountStore.getRevokingDB().put(address, account.toByteArray());
              writeCount.incrementAndGet();
            }
          } catch (InterruptedException e) {
            logger.error("account convert asset exception: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
          }
        });
        writeFutures.add(future);
      }
      for (int i = 0; i < writeFutures.size(); i++) {
        writeFutures.get(i).get();
      }
      writeCost.set(System.currentTimeMillis() - start);
    }

    public void convertAccountAssetIssueToAccount() {
      List<Future<?>> writeFutures = new ArrayList<>();
      ExecutorService writeExecutor = Executors.newFixedThreadPool(ThreadPoolUtil.getMaxPoolSize());
      for (int i = 0; i < ThreadPoolUtil.getMaxPoolSize(); i++) {
        Future<?> future = writeExecutor.submit(() -> {
          try {
            while (true) {
              Map.Entry<byte[], byte[]> accountAssetIssue = convertQueue.take();
              AccountAssetIssueCapsule accountAssetIssueCapsule =
                new AccountAssetIssueCapsule(accountAssetIssue.getValue());
              byte[] address = accountAssetIssueCapsule.getAddress().toByteArray();
              AccountCapsule accountCapsule = accountStore.get(address);
              Account account = accountCapsule.getInstance()
                .toBuilder()
                .setAddress(accountAssetIssueCapsule.getAddress())
                .setAssetIssuedID(accountAssetIssueCapsule.getAssetIssuedID())
                .setAssetIssuedName(accountAssetIssueCapsule.getAssetIssuedName())
                .putAllAsset(accountAssetIssueCapsule.getAssetMap())
                .putAllAssetV2(accountAssetIssueCapsule.getAssetMapV2())
                .putAllFreeAssetNetUsage(accountAssetIssueCapsule.getAllFreeAssetNetUsage())
                .putAllFreeAssetNetUsageV2(accountAssetIssueCapsule.getAllFreeAssetNetUsageV2())
                .putAllLatestAssetOperationTime(
                  accountAssetIssueCapsule.getLatestAssetOperationTimeMap())
                .putAllLatestAssetOperationTimeV2(
                  accountAssetIssueCapsule.getLatestAssetOperationTimeMapV2())
                .build();
              accountCapsule.setInstance(account);
              accountStore.put(address, accountCapsule);
            }
          } catch (InterruptedException e) {
            logger.error("convert account asset assue to account exception: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
          }
        });
        writeFutures.add(future);
      }
      for (int i = 0; i < writeFutures.size(); i++) {
        writeFutures.get(i);
      }
    }
  }

}
