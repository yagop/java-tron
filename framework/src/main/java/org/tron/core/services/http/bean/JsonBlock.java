package org.tron.core.services.http.bean;

import lombok.Data;
import org.tron.core.services.http.utils.JsonRpcUtils;
import org.tron.protos.Protocol;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Data
public class JsonBlock {

  private String number;

  private String hash, parentHash;

  private String nonce;

  private String sha3Uncles;

  private String logsBloom;

  private String transactionsRoot, stateRoot, receiptsRoot;

  private String miner;

  private String difficulty, totalDifficulty;

  private String extraData, size;

  private String gasLimit, gasUsed;

//  private String baseFeePerGas;

  private String timestamp;

  private List<String> transactions = new ArrayList<>();

  private List<String> uncles = new ArrayList<>();

  public static JsonBlock fromBlockCapsule(Protocol.Block block) {
    if (block == null) {
      return null;
    }
    Protocol.BlockHeader header = block.getBlockHeader();
    JsonBlock jsonBlock = new JsonBlock();
    jsonBlock.number = JsonRpcUtils.toHex(header.getRawData().getNumber());
    jsonBlock.hash = JsonRpcUtils.getBlockHash(block);
    jsonBlock.parentHash = JsonRpcUtils.toHex(header.getRawData().getParentHash());
    jsonBlock.nonce = JsonRpcUtils.toHex(0);
    jsonBlock.sha3Uncles = JsonRpcUtils.emptyHex();
    jsonBlock.logsBloom = JsonRpcUtils.toHex(new byte[256]);
    jsonBlock.transactionsRoot = JsonRpcUtils.toHex(header.getRawData().getTxTrieRoot());
    jsonBlock.stateRoot = JsonRpcUtils.toHex(header.getRawData().getAccountStateRoot());
    jsonBlock.receiptsRoot = JsonRpcUtils.toHex(new byte[32]);
    jsonBlock.miner = JsonRpcUtils.toEthAddress(header.getRawData().getWitnessAddress());
    jsonBlock.difficulty = JsonRpcUtils.toHex(0);
    jsonBlock.totalDifficulty = JsonRpcUtils.toHex(0);
    jsonBlock.extraData = JsonRpcUtils.emptyHex();
    jsonBlock.size = JsonRpcUtils.toHex(header.getRawData().getSerializedSize());
    jsonBlock.gasLimit = JsonRpcUtils.toHex(30_000_000);
    jsonBlock.gasUsed = JsonRpcUtils.toHex(0);
//    jsonBlock.baseFeePerGas = JsonRpcUtils.toHex(BigInteger.valueOf(140).multiply(BigInteger.valueOf(10).pow(9)).toByteArray());
    jsonBlock.timestamp = JsonRpcUtils.toHex(header.getRawData().getTimestamp() / 1000);
    block.getTransactionsList().forEach(t ->
        jsonBlock.getTransactions().add(JsonRpcUtils.sha256(t.getRawData().toByteArray())));
    return jsonBlock;
  }
}
