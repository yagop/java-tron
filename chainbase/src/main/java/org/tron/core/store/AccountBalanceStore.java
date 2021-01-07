package org.tron.core.store;

import com.google.protobuf.ByteString;
import com.typesafe.config.ConfigObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.Commons;
import org.tron.core.capsule.AccountBalanceCapsule;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.db.accountstate.AccountStateCallBackUtils;
import org.tron.core.db2.common.IRevokingDB;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j(topic = "DB")
@Component
public class AccountBalanceStore extends TronStoreWithRevoking<AccountBalanceCapsule> {

    @Autowired
    private AccountStore accountStore;

    @Autowired
    private DynamicPropertiesStore dynamicPropertiesStore;

    private static Map<String, byte[]> assertsAddress = new HashMap<>(); // key = name , value = address

    @Autowired
    private AccountStateCallBackUtils accountStateCallBackUtils;

    @Autowired
    private AccountBalanceStore(@Value("account-balance") String dbName) {
        super(dbName);
    }

    //插入一个空账户，用于启动判断是否结束
    public void convertToAccountBalance() {
        logger.info("Check if synchronization is required: {}", dynamicPropertiesStore.getAccountBalanceConvert());
        if (dynamicPropertiesStore.getAccountBalanceConvert() == 0) {
            long start = System.currentTimeMillis();
            Timer timer = countDown();
            int count = 0;
            for (Map.Entry<byte[], byte[]> entry : accountStore.getRevokingDB()) {
                AccountCapsule accountCapsule = new AccountCapsule(entry.getValue());
                this.put(entry.getKey(), new AccountBalanceCapsule(
                        accountCapsule.getAddress(),
                        accountCapsule.getOriginalBalance(),
                        accountCapsule.getType()));
                count++;
            }

            logger.info("import balance time: {}, count{}", (System.currentTimeMillis() - start) / 1000, count);
            dynamicPropertiesStore.setAccountBalanceConvert(1);
            timer.cancel();
        } else {
            logger.info("No synchronization required");
        }
    }

    public Timer countDown() {
        Timer timer = new Timer();
        AtomicInteger count = new AtomicInteger();
        timer.schedule(new TimerTask(){
            public void run(){
                int second = count.incrementAndGet();
                if (second % 5 == 0) {
                    logger.info("import balance current second: {} S", second);
                };
            }
        }, 0, 1000);
        return timer;
    }

    public static void setAccount(com.typesafe.config.Config config) {
        List list = config.getObjectList("genesis.block.assets");
        for (int i = 0; i < list.size(); i++) {
            ConfigObject obj = (ConfigObject) list.get(i);
            String accountName = obj.get("accountName").unwrapped().toString();
            byte[] address = Commons.decodeFromBase58Check(obj.get("address").unwrapped().toString());
            assertsAddress.put(accountName, address);
        }
    }

    @Override
    public AccountBalanceCapsule get(byte[] key) {
        byte[] value = revokingDB.getUnchecked(key);
        return ArrayUtils.isEmpty(value) ? null : new AccountBalanceCapsule(value);
    }

    @Override
    public void put(byte[] key, AccountBalanceCapsule accountBalanceCapsule) {
        super.put(key, accountBalanceCapsule);
        accountStateCallBackUtils.accountBalanceCallBack(key, accountBalanceCapsule);
    }

    public void put(byte[] key, AccountCapsule accountCapsule) {
        AccountBalanceCapsule accountBalanceCapsule = accountCapsule.getAccountBalanceCapsule();
        if (accountBalanceCapsule == null) {
            accountBalanceCapsule = new AccountBalanceCapsule(ByteString.copyFrom(key), accountCapsule.getType(), accountCapsule.getOriginalBalance());
        }
        put(key, accountBalanceCapsule);
    }

    /**
     * Min TRX account.
     */
    public AccountBalanceCapsule getBlackhole() {
        return getUnchecked(assertsAddress.get("Blackhole"));
    }

    public static class AccountConvertQueue {

        private BlockingQueue<Map.Entry<byte[], byte[]>> convertQueue;

        private AccountStore accountStore;

        private AccountBalanceStore accountBalanceStore;

        private ThreadPoolExecutor threadPoolExecutor;

        private static final int CORE_POOL_SIZE = 4;

        public static final int MAX_POOL_SIZE = 8;

        public static final int KEEP_ALIVE_SECONDES = 30;

        public AccountConvertQueue(BlockingQueue<Map.Entry<byte[], byte[]>> convertQueue) {
            this.convertQueue = convertQueue;
        }

        public AccountConvertQueue(BlockingQueue<Map.Entry<byte[], byte[]>> convertQueue, AccountStore accountStore, AccountBalanceStore accountBalanceStore) {
            this.convertQueue = convertQueue;
            this.accountStore = accountStore;
            this.accountBalanceStore = accountBalanceStore;
        }

        public void convert() {
            threadPoolExecutor = getThreadPoolExecutor();
            for (int i = 0; i < MAX_POOL_SIZE; i++) {
                threadPoolExecutor.execute(()-> {
                    try {
                        while (true) {
                            Map.Entry<byte[], byte[]> accountEntry = convertQueue.take();
                            AccountCapsule account = new AccountCapsule(accountEntry.getValue());
                            byte[] addressByte = account.getAddress().toByteArray();
                            accountBalanceStore.put(addressByte, new AccountBalanceCapsule(
                                    account.getAddress(),
                                    account.getOriginalBalance(),
                                    account.getType()));
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        private ThreadPoolExecutor getThreadPoolExecutor() {
            return new ThreadPoolExecutor(
                    CORE_POOL_SIZE,
                    MAX_POOL_SIZE,
                    KEEP_ALIVE_SECONDES,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(1000)
            );
        }

        public void closeTask() {
            threadPoolExecutor.shutdown();
        }
    }

    public static class AccountRecordQueue {

        private final CountDownLatch countDownLatch;

        private BlockingQueue productQueue;

        public AccountRecordQueue(BlockingQueue<Map.Entry<byte[], byte[]>> productQueue, CountDownLatch countDownLatch) {
            this.productQueue = productQueue;
            this.countDownLatch = countDownLatch;
        }

        public void put(Map.Entry<byte[], byte[]> accountByte) {
            try {
                productQueue.put(accountByte);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void fetchAccount (IRevokingDB revokingDB) {
            new Thread(()->{
                for (Map.Entry<byte[], byte[]> accountRecore : revokingDB) {
                    put(accountRecore);
                }
                countDownLatch.countDown();
            }).start();
        }
    }


    public static class BlockQueueFactory {

        private static BlockingQueue queue;

        public static BlockingQueue getInstance() {
            if (null == queue) {
                synchronized (BlockQueueFactory.class) {
                    if (null == queue) {
                        queue = new LinkedBlockingDeque(10000000);
                    }
                }
            }
            return queue;
        }

        public BlockingQueue getInstance(int capcity) {
            if (null == queue) {
                synchronized (this) {
                    if (null == queue) {
                        queue = new LinkedBlockingDeque<>(capcity);
                    }
                }
            }
            return queue;
        }
    }
}
