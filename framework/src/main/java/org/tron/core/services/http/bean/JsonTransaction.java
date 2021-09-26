package org.tron.core.services.http.bean;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Data;
import org.tron.core.Wallet;
import org.tron.core.services.http.utils.JsonRpcUtils;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass.EthTransaction;

@Data
public class JsonTransaction {

  private String blockHash, blockNumber;

  private String from;

  private String gas, gasPrice;

  private String hash;

  private String input;

  private String nonce;

  private String to;

  private String transactionIndex;

  private String value;

  private String v, r, s;

  public static JsonTransaction fromTransactionAndInfo(Protocol.Transaction tx,
      Protocol.TransactionInfo info, Wallet wallet) {
    if (tx == null || info == null) {
      return null;
    }
    JsonTransaction jsonTx = new JsonTransaction();
    jsonTx.blockHash = JsonRpcUtils.getBlockHash(wallet.getBlockByNum(info.getBlockNumber()));
    jsonTx.blockNumber = JsonRpcUtils.toHex(info.getBlockNumber());
    jsonTx.hash = JsonRpcUtils.toHex(info.getId());
    jsonTx.transactionIndex = JsonRpcUtils.toHex(0);
    try {
      EthTransaction ethTx = tx.getRawData().getContract(0)
          .getParameter().unpack(EthTransaction.class);
      jsonTx.from = JsonRpcUtils.toEthAddress(ethTx.getOwnerAddress());
      jsonTx.gas = JsonRpcUtils.toHex(ethTx.getGas());
      jsonTx.gasPrice = JsonRpcUtils.toHex(ethTx.getGasPrice());
      jsonTx.input = JsonRpcUtils.toHex(ethTx.getData());
      jsonTx.nonce = JsonRpcUtils.toHex(ethTx.getNonce());
      jsonTx.to = JsonRpcUtils.toHex(ethTx.getTo());
      jsonTx.value = JsonRpcUtils.toHex(ethTx.getValue());
      jsonTx.v = JsonRpcUtils.toHex(ethTx.getV());
      jsonTx.r = JsonRpcUtils.toHex(ethTx.getR());
      jsonTx.s = JsonRpcUtils.toHex(ethTx.getS());
    } catch (InvalidProtocolBufferException e) {
      return null;
    }
    return jsonTx;
  }
}
