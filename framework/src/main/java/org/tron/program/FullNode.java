package org.tron.program;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.PriorityBlockingQueue;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionRetCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.http.FullNodeHttpApiService;
import org.tron.core.services.interfaceOnPBFT.RpcApiServiceOnPBFT;
import org.tron.core.services.interfaceOnPBFT.http.PBFT.HttpApiOnPBFTService;
import org.tron.core.services.interfaceOnSolidity.RpcApiServiceOnSolidity;
import org.tron.core.services.interfaceOnSolidity.http.solidity.HttpApiOnSolidityService;
import org.tron.protos.Protocol;

@Slf4j(topic = "app")
public class FullNode {
  
  public static final int dbVersion = 2;

  public static void load(String path) {
    try {
      File file = new File(path);
      if (!file.exists() || !file.isFile() || !file.canRead()) {
        return;
      }
      LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
      JoranConfigurator configurator = new JoranConfigurator();
      configurator.setContext(lc);
      lc.reset();
      configurator.doConfigure(file);
    } catch (Exception e) {
      logger.error(e.getMessage());
    }
  }

  /**
   * Start the FullNode.
   */
  public static void main(String[] args) {
    logger.info("Full node running.");
    Args.setParam(args, Constant.TESTNET_CONF);
    CommonParameter parameter = Args.getInstance();

    load(parameter.getLogbackPath());

    if (parameter.isHelp()) {
      logger.info("Here is the help message.");
      return;
    }

    if (Args.getInstance().isDebug()) {
      logger.info("in debug mode, it won't check energy time");
    } else {
      logger.info("not in debug mode, it will check energy time");
    }

    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    beanFactory.setAllowCircularReferences(false);
    TronApplicationContext context =
        new TronApplicationContext(beanFactory);
    context.register(DefaultConfig.class);

    context.refresh();
    Application appT = ApplicationFactory.create(context);
    shutdown(appT);

    // grpc api server
    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
    appT.addService(rpcApiService);

    // http api server
    FullNodeHttpApiService httpApiService = context.getBean(FullNodeHttpApiService.class);
    if (CommonParameter.getInstance().fullNodeHttpEnable) {
      appT.addService(httpApiService);
    }

    // full node and solidity node fuse together
    // provide solidity rpc and http server on the full node.
    if (Args.getInstance().getStorage().getDbVersion() == dbVersion) {
      RpcApiServiceOnSolidity rpcApiServiceOnSolidity = context
          .getBean(RpcApiServiceOnSolidity.class);
      appT.addService(rpcApiServiceOnSolidity);
      HttpApiOnSolidityService httpApiOnSolidityService = context
          .getBean(HttpApiOnSolidityService.class);
      if (CommonParameter.getInstance().solidityNodeHttpEnable) {
        appT.addService(httpApiOnSolidityService);
      }
    }

    // PBFT API (HTTP and GRPC)
    if (Args.getInstance().getStorage().getDbVersion() == dbVersion) {
      RpcApiServiceOnPBFT rpcApiServiceOnPBFT = context
          .getBean(RpcApiServiceOnPBFT.class);
      appT.addService(rpcApiServiceOnPBFT);
      HttpApiOnPBFTService httpApiOnPBFTService = context
          .getBean(HttpApiOnPBFTService.class);
      appT.addService(httpApiOnPBFTService);
    }

//    appT.initServices(parameter);
//    appT.startServices();
//    appT.startup();

    long latestBlockNum = appT.getChainBaseManager()
        .getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    CountDownLatch counter = new CountDownLatch(4);
    Queue<Item> queue = new PriorityBlockingQueue<>(1000, (i1, i2) -> (int) (i1.energy - i2.energy));
    new Thread(new Task(
        appT.getChainBaseManager(),
        latestBlockNum,
        5_000_000,
        counter,
        queue), "Traversal-1").start();
    latestBlockNum -= 5_000_000;
    new Thread(new Task(
        appT.getChainBaseManager(),
        latestBlockNum,
        5_000_000,
        counter,
        queue), "Traversal-2").start();
    latestBlockNum -= 5_000_000;
    new Thread(new Task(
        appT.getChainBaseManager(),
        latestBlockNum,
        8_000_000,
        counter,
        queue), "Traversal-3").start();
    latestBlockNum -= 8_000_000;
    new Thread(new Task(
        appT.getChainBaseManager(),
        latestBlockNum,
        8_000_000,
        counter,
        queue), "Traversal-4").start();
    latestBlockNum -= 8_000_000;
    new Thread(new Task(
        appT.getChainBaseManager(),
        latestBlockNum,
        8_000_000,
        counter,
        queue), "Traversal-5").start();

    rpcApiService.blockUntilShutdown();
  }

  private static class Item {

    byte[] txID;
    long energy;
    long fee;
    Protocol.Transaction.Result.contractResult result;

    public Item(byte[] txID, long energy, long fee, Protocol.Transaction.Result.contractResult result) {
      this.txID = txID;
      this.energy = energy;
      this.fee = fee;
      this.result = result;
    }
  }

  private static class Task implements Runnable {

    private final ChainBaseManager manager;

    private final long startIndex;

    private final long totalScan;

    private final CountDownLatch counter;

    private final Queue<Item> queue;

    public Task(ChainBaseManager manager,
                long startIndex,
                long totalScan,
                CountDownLatch counter,
                Queue<Item> queue) {
      this.manager = manager;
      this.startIndex = startIndex;
      this.totalScan = totalScan;
      this.counter = counter;
      this.queue = queue;
    }

    @Override
    public void run() {
      String name = Thread.currentThread().getName();
      System.out.println(name + " start: " + startIndex + " " + totalScan);
      long start = System.currentTimeMillis(), total = 0;
      for (int i = 1; i <= totalScan; i++) {
        if (i % 10000 == 0) {
          System.out.println(name + ": " + i + " cost " + ((System.currentTimeMillis() - start) / 1000) + "s");
          Item item = queue.peek();
          if (item != null) {
            System.out.println(name + ": " + Hex.toHexString(item.txID) + " " + item.energy + " " + item.fee + "trx " + item.result);
          }
          start = System.currentTimeMillis();
        }
        TransactionRetCapsule ret = null;
        try {
          ret = manager.getTransactionRetStore().getTransactionInfoByBlockNum(
              ByteArray.fromLong(startIndex - i));
        } catch (Exception e) {
          e.printStackTrace();
        }
        if (ret != null) {
          for (Protocol.TransactionInfo info : ret.getInstance().getTransactioninfoList()) {
            info.getLogList().forEach(log -> {
              if (log.getTopicsList().size() > 0 && Hex.toHexString(log.getTopics(0).toByteArray()).toLowerCase().contains("deaa91b6")) {
                System.out.println(Hex.toHexString(info.getId().toByteArray()));
              }
            });
//            if (info.getInternalTransactionsCount() > 60) {
//              System.out.println(Hex.toHexString(info.getId().toByteArray()) + ": " + info.getInternalTransactionsCount());
//            }
//            if (!info.getContractAddress().isEmpty()) total += 1;
//            if (!info.getContractAddress().isEmpty()
//                && (info.getReceipt().getResult() == Protocol.Transaction.Result.contractResult.SUCCESS
//                || info.getReceipt().getResult() == Protocol.Transaction.Result.contractResult.REVERT)) {
//              long energy = info.getReceipt().getEnergyUsageTotal();
//              if (queue.size() < 1000) {
//                queue.offer(new Item(info.getId().toByteArray(), energy,
//                    info.getReceipt().getEnergyFee() / 1000 / 1000, info.getReceipt().getResult()));
//              } else if (queue.peek().energy < energy) {
//                queue.poll();
//                queue.offer(new Item(info.getId().toByteArray(), energy,
//                    info.getReceipt().getEnergyFee() / 1000 / 1000, info.getReceipt().getResult()));
//              }
//            }
          }
        }
      }
      System.out.println(name + " done: " + total);
//      if (counter.getCount() == 0) {
//        int size = queue.size();
//        for (int i = 0; i < size; i++) {
//          Item item = queue.poll();
//          if (item != null) {
//            System.out.print(Hex.toHexString(item.txID) + ": " + item.energy + " " + item.fee + "trx " + item.result + " ");
//            TransactionCapsule tx = null;
//            try {
//              tx = manager.getTransactionStore().get(item.txID);
//            } catch (Exception e) {
//              e.printStackTrace();
//            }
//            if (tx != null) {
//              System.out.println(tx.getInstance().getRawData().getContract(0).getType() + " " +
//                  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
//                      .format(tx.getInstance().getRawData().getTimestamp()));
//            } else {
//              System.out.println("unknown");
//            }
//          }
//        }
//        System.exit(0);
//      } else {
//        counter.countDown();
//      }
    }
  }

  public static void shutdown(final Application app) {
    logger.info("********register application shutdown hook********");
    Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
  }
}
