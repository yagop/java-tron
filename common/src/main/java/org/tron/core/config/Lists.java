package org.tron.core.config;

import com.google.common.base.Charsets;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.StringUtil;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass;

@Component
@Slf4j(topic = "utils")
public class Lists {
  public static final Set<ByteString> BLOCKLIST = new HashSet<>();

  @PostConstruct
  public void load() {
    String blocklistPath = CommonParameter.getInstance().getBlocklistPath();
    if (StringUtils.isEmpty(blocklistPath)) {
      return;
    }

    try {
      List<String> addressList =
          FileUtils.readLines(new File(blocklistPath), Charsets.UTF_8).stream()
              .filter(StringUtils::isNotBlank)
              .map(String::trim)
              .collect(Collectors.toList());
      for (String s : addressList) {
        byte[] bytes = StringUtil.decodeFromBase58Check(s);
        if (bytes == null) {
          throw new IllegalArgumentException("this address format is not base58, please check it. "
              + s);
        }
        ByteString bs = ByteString.copyFrom(bytes);
        BLOCKLIST.add(bs);
      }
    } catch (IOException | IllegalArgumentException e) {
      logger.error("the file has some errors, please check it. " + e.getMessage(), e);
      System.exit(-1);
    }
  }

  public static boolean containsWithBlocklist(ByteString address) {
    return BLOCKLIST.contains(address);
  }

  public static boolean containsWithBlocklist(Protocol.Transaction transaction) {
    Protocol.Transaction.Contract.ContractType contractType =
        transaction.getRawData().getContract(0).getType();
    if (!contractType.equals(Protocol.Transaction.Contract.ContractType.TriggerSmartContract)) {
      return false;
    }

    Any any = transaction.getRawData().getContract(0).getParameter();
    try {
      SmartContractOuterClass.TriggerSmartContract triggerSmartContract = any.unpack(
          SmartContractOuterClass.TriggerSmartContract.class);
      return containsWithBlocklist(triggerSmartContract.getContractAddress());
    } catch (InvalidProtocolBufferException e) {
      return false;
    }
  }
}
