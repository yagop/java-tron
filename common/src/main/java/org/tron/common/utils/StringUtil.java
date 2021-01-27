/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.utils;

import com.google.protobuf.ByteString;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.parameter.CommonParameter;

public class StringUtil {

  public static byte[] createDbKey(ByteString string) {
    return string.toByteArray();
  }

  public static String createReadableString(byte[] bytes) {
    return ByteArray.toHexString(bytes);
  }

  public static String encode58Check(byte[] input) {
    byte[] hash0 = Sha256Hash.hash(CommonParameter.getInstance().isECKeyCryptoEngine(), input);
    byte[] hash1 = Sha256Hash.hash(CommonParameter.getInstance().isECKeyCryptoEngine(), hash0);
    byte[] inputCheck = new byte[input.length + 4];
    System.arraycopy(input, 0, inputCheck, 0, input.length);
    System.arraycopy(hash1, 0, inputCheck, input.length, 4);
    return Base58.encode(inputCheck);
  }

  public static byte[] decode58Check(String input) {
    byte[] decodeCheck = Base58.decode(input);
    if (decodeCheck.length <= 4) {
      return null;
    }
    byte[] decodeData = new byte[decodeCheck.length - 4];
    System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
    byte[] hash0 = Sha256Hash.hash(CommonParameter.getInstance().isECKeyCryptoEngine(),
        decodeData);
    byte[] hash1 = Sha256Hash.hash(CommonParameter.getInstance().isECKeyCryptoEngine(),
        hash0);
    if (hash1[0] == decodeCheck[decodeData.length] &&
        hash1[1] == decodeCheck[decodeData.length + 1] &&
        hash1[2] == decodeCheck[decodeData.length + 2] &&
        hash1[3] == decodeCheck[decodeData.length + 3]) {
      return decodeData;
    }
    return null;
  }

  public static byte[] decodeFromBase58Check(String addressBase58) {
    if (StringUtils.isEmpty(addressBase58)) {
      return null;
    }
    byte[] address = decode58Check(addressBase58);
    if (address == null) {
      return null;
    }

    if (!DecodeUtil.addressValid(address)) {
      return null;
    }

    return address;
  }

  public static String createReadableString(ByteString string) {
    return createReadableString(string.toByteArray());
  }

  public static ByteString hexString2ByteString(String hexString) {
    return ByteString.copyFrom(ByteArray.fromHexString(hexString));
  }
}
