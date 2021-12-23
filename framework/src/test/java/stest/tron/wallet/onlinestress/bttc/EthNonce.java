package stest.tron.wallet.onlinestress.bttc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.core.Wallet;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.http.HttpService;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class EthNonce {

  private final String testAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] testAccountAddress = PublicMethed.getFinalAddress(testAccountKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String UniswapV2Router02 = "TS5KBayVMvnvMSLKEU2Hw2gmJbcN1FpNdW";
  private String SunMaker = "TS5KBayVMvnvMSLKEU2Hw2gmJbcN1FpNdW";
  private String SunBar = "TYjErPGGmrfb34FUUjT2rDHc3xTNj6M9DU";
  private String USDJ_SUN = "TJeVLpnYhLPu17hckRCUxaEcuzcGW2MneS";
  private String USDJ = "TLBaRhANQoJFTqre9Nf1mjuwNWjCJeYqUL";
  private String SUN = "TDqjTkZ63yHB19w2n7vPm2qAkLHwn9fKKk";
  private String feeTo = "TRxXEwPDCUwS9bB9d9tz2QawkmPLkLUpEF";
  private String account2 = "TGQs89kSUYVmAFULRngL6QJiCGQUpqYTad";

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(threadPoolSize = 1, invocationCount = 1)
  public void depositEth() throws Exception {
    String from = "0xDDCE857A4A507E9DA633172A18A0BF6C5B451033";
    Web3j web3j = Web3j.build(new HttpService("https://rpc.bt.io/"));

    EthGetTransactionCount ethGetTransactionCount = web3j
        .ethGetTransactionCount(from, DefaultBlockParameterName.PENDING).send();
    BigInteger nonce = ethGetTransactionCount.getTransactionCount();

    System.out.println("nonce.intValue():" + nonce.intValue());
  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
