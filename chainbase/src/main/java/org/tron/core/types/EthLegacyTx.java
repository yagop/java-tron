package org.tron.core.types;

import com.google.protobuf.ByteString;
import lombok.Data;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Hash;
import org.tron.common.rlp.RLP;
import org.tron.common.rlp.RLPList;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass.EthTransaction;

import java.math.BigInteger;
import java.util.Arrays;

@Data
public class EthLegacyTx {

  private static final int CHAIN_ID_INC = 35;
  private static final int LOWER_REAL_V = 27;
  private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

  private byte[] owner;

  private byte[] nonce;

  private byte[] gasPrice;

  private byte[] gas;

  private byte[] to;

  private byte[] value;

  private byte[] data;

  private byte[] v, r, s;

  private byte[] txHash;

  private byte[] from;

  public static Protocol.Transaction toTronTx() {
    Protocol.Transaction.Builder tronTx = Protocol.Transaction.newBuilder();
    return tronTx.build();
  }

  public static EthLegacyTx fromLegacy(RLPList tx) {
    if (tx.size() != 9) {
      return null;
    }
    EthLegacyTx txData = new EthLegacyTx();
    txData.nonce = tx.get(0).getRLPData();
    txData.gasPrice = tx.get(1).getRLPData();
    txData.gas = tx.get(2).getRLPData();
    txData.to = tx.get(3).getRLPData();
    txData.value = tx.get(4).getRLPData();
    txData.data = tx.get(5).getRLPData();
    txData.v = tx.get(6).getRLPData();
    txData.r = tx.get(7).getRLPData();
    txData.s = tx.get(8).getRLPData();
    txData.txHash = Hash.sha3(txData.getEncodedRaw());
    return txData;
  }

  public static EthLegacyTx fromEthTransaction(EthTransaction tx) {
    EthLegacyTx txData = new EthLegacyTx();
    txData.owner = tx.getOwnerAddress().toByteArray();
    txData.nonce = tx.getNonce().toByteArray();
    txData.gasPrice = tx.getGasPrice().toByteArray();
    txData.gas = tx.getGas().toByteArray();
    txData.to = tx.getTo().toByteArray();
    txData.value = tx.getValue().toByteArray();
    txData.data = tx.getData().toByteArray();
    txData.v = tx.getV().toByteArray();
    txData.r = tx.getR().toByteArray();
    txData.s = tx.getS().toByteArray();
    txData.txHash = Hash.sha3(txData.getEncodedRaw());
    return txData;
  }

  public EthTransaction toEthTransaction() {
    EthTransaction.Builder builder = EthTransaction.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(convertToTron(recoverFrom())))
        .setGasPrice(ByteString.copyFrom(gasPrice))
        .setGas(ByteString.copyFrom(gas))
        .setV(ByteString.copyFrom(v))
        .setR(ByteString.copyFrom(r))
        .setS(ByteString.copyFrom(s));

    if (notEmpty(nonce)) {
      builder.setNonce(ByteString.copyFrom(nonce));
    } else {
      builder.setNonce(ByteString.copyFrom(BigInteger.ZERO.toByteArray()));
    }

    if (notEmpty(to)) {
      builder.setTo(ByteString.copyFrom(to));
    }

    if (notEmpty(value)) {
      builder.setValue(ByteString.copyFrom(value));
    }

    if (notEmpty(data)) {
      builder.setData(ByteString.copyFrom(data));
    }

    return builder.build();
  }

  public byte[] recoverFrom() {
    byte v = getRealV();
    byte[] pubKey = ECKey.recoverPubBytesFromSignature(v - 27,
        ECKey.ECDSASignature.fromComponents(r, s, v), txHash);
    if (pubKey == null) {
      return null;
    }
    return from = Arrays.copyOfRange(
        Hash.sha3(Arrays.copyOfRange(pubKey, 1, pubKey.length)), 12, 32);
  }

  public byte getRealV() {
    BigInteger bv = new BigInteger(1, v);
    if (bv.bitLength() > 31) {
      return 0;
    }
    int v = bv.intValue();
    if (v == LOWER_REAL_V || v == (LOWER_REAL_V + 1)) {
      return (byte) v;
    }
    int inc = 0;
    if (v % 2 == 0) inc = 1;
    return (byte) ((byte) LOWER_REAL_V + inc);
  }

  private byte[] getEncodedRaw() {
    byte[] rlpRaw;

    // parse null as 0 for nonce
    byte[] nonce;
    if (this.nonce == null || this.nonce.length == 1 && this.nonce[0] == 0) {
      nonce = RLP.encodeElement(null);
    } else {
      nonce = RLP.encodeElement(this.nonce);
    }
    byte[] gasPrice = RLP.encodeElement(this.gasPrice);
    byte[] gas = RLP.encodeElement(this.gas);
    byte[] to = RLP.encodeElement(this.to);
    byte[] value = RLP.encodeElement(this.value);
    byte[] data = RLP.encodeElement(this.data);

    // TODO current only int chainid supported
    int chainId = deriveChainId();
    if (chainId == 0) {
      rlpRaw = RLP.encodeList(nonce, gasPrice, gas, to, value, data);
    } else {
      byte[] v, r, s;
      v = RLP.encodeInt(chainId);
      r = RLP.encodeElement(EMPTY_BYTE_ARRAY);
      s = RLP.encodeElement(EMPTY_BYTE_ARRAY);
      rlpRaw = RLP.encodeList(nonce, gasPrice, gas, to, value, data, v, r, s);
    }
    return rlpRaw;
  }

  private boolean notEmpty(byte[] data) {
    return data != null && data.length != 0;
  }

  private byte[] convertToTron(byte[] ethAddress) {
    byte[] tronAddress = new byte[21];
    tronAddress[0] = 0x41;
    System.arraycopy(ethAddress, 0, tronAddress, 1, 20);
    return tronAddress;
  }

  private int deriveChainId() {
    BigInteger bv = new BigInteger(1, v);
    int v = bv.intValue();
    if (v == LOWER_REAL_V || v == (LOWER_REAL_V + 1)) {
      return 0;
    }
    return (v - CHAIN_ID_INC) / 2;
  }
}
