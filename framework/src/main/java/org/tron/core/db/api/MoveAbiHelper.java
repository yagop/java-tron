package org.tron.core.db.api;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.StringUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AbiCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.store.AbiStore;
import org.tron.core.store.ContractStore;

@Slf4j(topic = "DB")
public class MoveAbiHelper {

  private int count;

  private final ChainBaseManager chainBaseManager;

  public MoveAbiHelper(ChainBaseManager chainBaseManager) {
    this.chainBaseManager = chainBaseManager;
  }

  public void doWork() {
    batchContractStore();
    long start = System.currentTimeMillis();
    logger.info("Start to move abi");
    AbiStore abiStore = chainBaseManager.getAbiStore();
    ContractStore contractStore = chainBaseManager.getContractStore();
    Iterator<Map.Entry<byte[], ContractCapsule>> it = contractStore.iterator();
    it.forEachRemaining(e -> {
      ContractCapsule contractCapsule = e.getValue();
      if (!contractCapsule.getInstance().getAbi().getEntrysList().isEmpty()) {
        abiStore.put(e.getKey(), contractCapsule.getInstance().getAbi().toByteArray());
      }
      contractStore.put(e.getKey(), contractCapsule);
      count += 1;
      if (count % 100_000 == 0) {
        logger.info("Doing the abi move, current count: {} {}", count,
            System.currentTimeMillis());
      }
    });
    chainBaseManager.getDynamicPropertiesStore().saveAbiMoveDone(1);
    logger.info(
        "Complete the abi move, total time: {} milliseconds, total count: {}",
        System.currentTimeMillis() - start, count);
    batchAbiStore();
  }

  public void batchContractStore() {
    ContractStore contractStore = chainBaseManager.getContractStore();
    Iterator<Map.Entry<byte[], ContractCapsule>> it = contractStore.iterator();
    it.forEachRemaining(co -> {
      String key = StringUtil.encode58Check(co.getKey());
      String res = "\nkey: "+key + "\nvalue: "+co.getValue().getInstance().getAbi().toString();
      File file = new File("/data/databackup/workspace_TRC20/TRON/contractStroe.txt");
      FileWriter writer;
      try {
        if(!file.exists()){
          file.createNewFile();
        }
        writer = new FileWriter(file, true);
        writer.write(res);
        writer.flush();
        writer.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }

  public void batchAbiStore() {
    AbiStore abiStore = chainBaseManager.getAbiStore();
    Iterator<Map.Entry<byte[], AbiCapsule>> it = abiStore.iterator();
    it.forEachRemaining(ab -> {
      String key = StringUtil.encode58Check(ab.getKey());
      String res = "\nkey: "+key + "\nvalue: "+ab.getValue().toString();
      File file = new File("/data/databackup/workspace_TRC20/TRON/abiStroe.txt");
      FileWriter writer;
      try {
        if(!file.exists()){
          file.createNewFile();
        }
        writer = new FileWriter(file, true);
        writer.write(res);
        writer.flush();
        writer.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }
}
