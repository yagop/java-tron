package org.tron.core.services.http.bean;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Data;
import org.tron.core.Wallet;
import org.tron.core.services.http.utils.JsonRpcUtils;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo.Log;
import org.tron.protos.contract.SmartContractOuterClass.EthTransaction;

import java.util.ArrayList;
import java.util.List;

@Data
public class JsonReceipt {

  @Data
  static class JsonLog {

    private String address;

    private String blockHash, blockNumber;

    private String data;

    private String logIndex;

    private boolean removed;

    private List<String> topics = new ArrayList<>();

    private String transactionHash;

    private String transactionIndex;

    JsonLog(Log log, JsonReceipt receipt, int index) {
      this.address = JsonRpcUtils.toEthAddress(log.getAddress());
      this.blockHash = receipt.blockHash;
      this.blockNumber = receipt.blockNumber;
      this.data = JsonRpcUtils.toHex(log.getData());
      this.logIndex = JsonRpcUtils.toHex(index);
      this.removed = false;
      log.getTopicsList().forEach(t -> topics.add(JsonRpcUtils.toHex(t)));
      this.transactionHash = receipt.transactionHash;
      this.transactionIndex = receipt.transactionIndex;
    }
  }

  private String transactionHash, transactionIndex;

  private String blockHash, blockNumber;

  private String from, to;

  private String cumulativeGasUsed, gasUsed;

  private String contractAddress;

  private List<JsonLog> logs = new ArrayList<>();

  private String logsBloom;

  private int status;

  public static JsonReceipt fromTransactionAndInfo(Protocol.Transaction tx,
      Protocol.TransactionInfo info, Wallet wallet) {
    if (tx == null || info == null) {
      return null;
    }
    JsonReceipt receipt = new JsonReceipt();
    receipt.transactionHash = JsonRpcUtils.toHex(info.getId());
    receipt.transactionIndex = JsonRpcUtils.toHex(0);
    receipt.blockHash = JsonRpcUtils.getBlockHash(wallet.getBlockByNum(info.getBlockNumber()));
    receipt.blockNumber = JsonRpcUtils.toHex(info.getBlockNumber());
    try {
      EthTransaction ethTx = tx.getRawData().getContract(0)
          .getParameter().unpack(EthTransaction.class);
      receipt.from = JsonRpcUtils.toEthAddress(ethTx.getOwnerAddress());
      receipt.to = JsonRpcUtils.toEthAddress(ethTx.getTo());
    } catch (InvalidProtocolBufferException e) {
      return null;
    }
    receipt.cumulativeGasUsed = JsonRpcUtils.toHex(0);
//    receipt.gasUsed = JsonRpcUtils.toHex(info.getReceipt().getEnergyUsageTotal());
    receipt.gasUsed = JsonRpcUtils.toHex(0);
    receipt.contractAddress = JsonRpcUtils.toEthAddress(info.getContractAddress());
    for (int i = 0; i < info.getLogList().size(); i++) {
      receipt.logs.add(new JsonLog(info.getLog(i), receipt, i));
    }
    receipt.logsBloom = JsonRpcUtils.toHex(new byte[256]);
    receipt.status = 1;
    return receipt;
  }
}
