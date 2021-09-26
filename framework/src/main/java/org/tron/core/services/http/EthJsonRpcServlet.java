package org.tron.core.services.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.utils.RLP;
import org.tron.core.capsule.utils.RLPList;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.services.http.bean.EthTxData;
import org.tron.core.services.http.bean.JsonBlock;
import org.tron.core.services.http.bean.JsonReceipt;
import org.tron.core.services.http.bean.JsonRpcRequest;
import org.tron.core.services.http.bean.JsonRpcResponse;
import org.tron.core.services.http.bean.JsonTransaction;
import org.tron.core.services.http.utils.JsonRpcUtils;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j(topic = "API")
public class EthJsonRpcServlet extends RateLimiterServlet {

  private static final BigInteger G = BigInteger.valueOf(10).pow(9);
  private static final BigInteger D = BigInteger.valueOf(10).pow(12);

  @Autowired
  private Wallet wallet;

  @Autowired
  private DynamicPropertiesStore dpStore;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    doProcess(request, response);
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    doProcess(request, response);
  }

  private void doProcess(HttpServletRequest request, HttpServletResponse response) {
    try {
      String data = request.getReader().lines().collect(Collectors.joining());
      JsonRpcRequest rpcRequest = JSONObject.parseObject(data, JsonRpcRequest.class);
      JsonRpcResponse rpcResponse = new JsonRpcResponse(rpcRequest);
      switch (rpcRequest.getMethod()) {
        case "web3_clientVersion":
          rpcResponse.setResult("Geth/v1.10.8-omnibus-aef5bfb3/linux-amd64/go1.16.7");
          break;
        case "net_version":
          rpcResponse.setResult(netVersion());
          break;
        case "eth_chainId":
          rpcResponse.setResult(chainId());
          break;
        case "eth_getBalance":
          rpcResponse.setResult(getBalance(rpcRequest.getParam(0)));
          break;
        case "eth_getCode":
          rpcResponse.setResult(getCode(rpcRequest.getParam(0)));
          break;
        case "eth_sendRawTransaction":
          Object result = sendRawTransaction(rpcRequest.getParam(0));
          if (result instanceof String && ((String) result).contains("code")) {
            rpcResponse.setError(result);
          } else {
            rpcResponse.setResult(result);
          }
          break;
        case "eth_blockNumber":
          rpcResponse.setResult(blockNumber());
          break;
        case "eth_gasPrice":
          rpcResponse.setResult(gasPrice());
          break;
        case "eth_estimateGas":
          rpcResponse.setResult(estimateGas());
          break;
        case "eth_getTransactionCount":
          rpcResponse.setResult(getTransactionCount(rpcRequest.getParam(0)));
          break;
        case "eth_getBlockByNumber":
          rpcResponse.setResult(getBlockByNumber(rpcRequest.getParam(0)));
          break;
        case "eth_getBlockByHash":
          rpcResponse.setResult(getBlockByHash(rpcRequest.getParam(0)));
          break;
        case "eth_getTransactionReceipt":
          rpcResponse.setResult(getTransactionReceipt(rpcRequest.getParam(0)));
          break;
        case "eth_getTransactionByHash":
          rpcResponse.setResult(getTransactionByHash(rpcRequest.getParam(0)));
          break;
        default:
          System.out.println(data);
          break;
      }
      response.addHeader("Content-Type", "application/json");
      String reqStr = JSONObject.toJSONString(rpcResponse, SerializerFeature.WriteMapNullValue);
      response.getWriter().println(reqStr);
      switch (rpcRequest.getMethod()) {
        case "eth_sendRawTransaction":
        case "eth_getBlockByHash":
        case "eth_getTransactionReceipt":
        case "eth_getCode":
        default:
          System.out.println(data);
          System.out.println(reqStr);
          System.out.println();
          break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private Object netVersion() {
    return "23333";
  }

  private Object chainId() {
    return "0x5b25";
  }

  private Object getBalance(String address) {
    Protocol.Account account = Protocol.Account.newBuilder()
        .setAddress(ByteString.copyFrom(appendTronPrefix(address)))
        .build();
    Protocol.Account reply = wallet.getAccount(account);
    long balance = 0;

    if (reply != null) {
      balance = reply.getBalance();
    }
    return JsonRpcUtils.toHex(BigInteger.valueOf(balance).multiply(D).toByteArray());
  }

  private Object getCode(String address) {
    SmartContractOuterClass.SmartContractDataWrapper contract = wallet.getContractInfo(
        GrpcAPI.BytesMessage.newBuilder()
            .setValue(ByteString.copyFrom(appendTronPrefix(address)))
            .build());
    if (contract == null) {
      return "0x";
    } else {
      return JsonRpcUtils.toHex(contract.getRuntimecode());
    }
  }

  private Set<String> cache = new HashSet<>();

  private Object sendRawTransaction(String rlpInput) {
    RLPList ethTx = null;
    EthTxData txData = null;
    byte[] input = Hex.decode(rlpInput.substring(2));
    if (input.length > 0 && bigger(input[0], 0x7f)) {
      // legacy transaction
      ethTx = (RLPList) RLP.decode2(input).get(0);
      txData = EthTxData.fromLegacy(ethTx);
    } else {
      ethTx = (RLPList) RLP.decode2(Arrays.copyOfRange(input, 1, input.length)).get(0);
      switch (input[0]) {
        case 1:
          // access list transaction
          break;
        case 2:
          // dynamic fee transaction
          break;
        default:
          //throw new ContractValidateException();
      }
    }
    if (txData == null) {

    } else {
      byte[] from = txData.recoverFrom();
      if (from == null) {

      } else {
        try {
          String key = Hex.toHexString(txData.getFrom()) + Hex.toHexString(txData.getNonce());
          if (cache.contains(key)) {
            return "{\"code\":-32603,\"data\":{\"code\":-32000,\"message\":\"oversized data\"}}";
          } else {
            cache.add(key);
          }
          TransactionCapsule tronTxCap = wallet.createTransactionCapsule(txData.toEthTransaction(),
              Protocol.Transaction.Contract.ContractType.EthTransaction);
          byte[] sig = new byte[65];
          System.arraycopy(txData.getR(), 0, sig, 0, 32);
          System.arraycopy(txData.getS(), 0, sig, 32, 32);
          sig[64] = txData.getRealV();
          Protocol.Transaction tronTx = tronTxCap.getInstance()
              .toBuilder().addSignature(ByteString.copyFrom(sig)).build();
          new Thread(() -> {
            try {
              Thread.sleep(10000);
              wallet.broadcastTransaction(tronTx);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }).start();
          return "0x" + Hex.toHexString(Sha256Hash.hash(true, tronTx.getRawData().toByteArray()));
        } catch (ContractValidateException e) {
          e.printStackTrace();
        }
      }
    }
    return "{\"code\":-32603,\"message\":\"no input\"}";
  }

  private Object gasPrice() {
    return JsonRpcUtils.toHex(BigInteger.valueOf(dpStore.getEnergyFee()).multiply(G).toByteArray());
  }

  private Object estimateGas() {
    return 100000;
  }

  private Object getTransactionCount(String address) {
    Protocol.Account account = Protocol.Account.newBuilder()
        .setAddress(ByteString.copyFrom(appendTronPrefix(address)))
        .build();
    Protocol.Account reply = wallet.getAccount(account);
    long nonce = 0;

    if (reply != null) {
      nonce = reply.getNonce();
    }
    return JsonRpcUtils.toHex(nonce);
  }

  private Object blockNumber() {
    return JsonRpcUtils.toHex(wallet.getBlockByLatestNum(1).getBlock(0)
        .getBlockHeader().getRawData().getNumber());
  }

  private Object getBlockByNumber(String number) {
    return JsonBlock.fromBlockCapsule(wallet.getBlockByNum(JsonRpcUtils.longFromString(number)));
  }

  private Object getBlockByHash(String hash) {
    ByteString blockHash = ByteString.copyFrom(Hex.decode(hash.substring(2)));
    return JsonBlock.fromBlockCapsule(wallet.getBlockById(blockHash));
  }

  private Object getTransactionReceipt(String hash) {
    ByteString txHash = ByteString.copyFrom(Hex.decode(hash.substring(2)));
    Protocol.Transaction tx = wallet.getTransactionById(txHash);
    Protocol.TransactionInfo info = wallet.getTransactionInfoById(txHash);
    return JsonReceipt.fromTransactionAndInfo(tx, info, wallet);
  }

  private Object getTransactionByHash(String hash) {
    ByteString txHash = ByteString.copyFrom(Hex.decode(hash.substring(2)));
    Protocol.Transaction tx = wallet.getTransactionById(txHash);
    Protocol.TransactionInfo info = wallet.getTransactionInfoById(txHash);
    return JsonTransaction.fromTransactionAndInfo(tx, info, wallet);
  }

  private boolean bigger(byte b, int h) {
    return (b & 0xff) > h;
  }

  private byte[] appendTronPrefix(String address) {
    if (address.contains("0x")) {
      address = address.substring(2);
    }
    byte[] tron = new byte[21];
    tron[0] = 0x41;
    System.arraycopy(Hex.decode(address), 0, tron, 1, 20);
    return tron;
  }
}
