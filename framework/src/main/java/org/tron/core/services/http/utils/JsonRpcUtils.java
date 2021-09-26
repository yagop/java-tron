package org.tron.core.services.http.utils;

import com.google.protobuf.ByteString;
import org.bouncycastle.util.encoders.Hex;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.protos.Protocol;

import java.math.BigInteger;
import java.util.Arrays;

public class JsonRpcUtils {

  private static final String HEX_PREFIX = "0x";

  private JsonRpcUtils() {}

  public static String emptyHex() {
    return HEX_PREFIX;
  }

  public static long longFromString(String num) {
    boolean isHex = false;
    if (num.contains("0x")) {
      num = num.substring(2);
      isHex = true;
    }
    try {
      return Long.parseLong(num, isHex ? 16 : 10);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  public static String toHex(long l) {
    return HEX_PREFIX + Hex.toHexString(BigInteger.valueOf(l).toByteArray());
  }

  public static String toHex(byte[] b) {
    if (b == null || b.length == 0) {
      return HEX_PREFIX;
    } else {
      return HEX_PREFIX + Hex.toHexString(b);
    }
  }

  public static String toHex(ByteString bs) {
    return toHex(bs.toByteArray());
  }

  public static String sha256(byte[] b) {
    return toHex(Sha256Hash.hash(true, b));
  }

  public static String sha256(ByteString bs) {
    return sha256(bs.toByteArray());
  }

  public static String toEthAddress(byte[] tronAddress) {
    if (tronAddress == null || (tronAddress.length != 20 && tronAddress.length != 21)) {
      return null;
    } else if (tronAddress.length == 20) {
      return toHex(tronAddress);
    } else {
      return toHex(Arrays.copyOfRange(tronAddress, 1, 21));
    }
  }

  public static String toEthAddress(ByteString tronAddress) {
    return toEthAddress(tronAddress.toByteArray());
  }

  public static String toTronAddress(byte[] ethAddress) {
    if (ethAddress == null || ethAddress.length != 20) {
      return null;
    } else {
      return HEX_PREFIX + "41" + toHex(ethAddress);
    }
  }

  public static String getBlockHash(Protocol.Block block) {
    return toHex(new BlockCapsule(block).getBlockId().getBytes());
  }
}
