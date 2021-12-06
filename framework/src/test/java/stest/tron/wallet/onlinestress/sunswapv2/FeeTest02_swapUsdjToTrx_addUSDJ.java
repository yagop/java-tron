package stest.tron.wallet.onlinestress.sunswapv2;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class FeeTest02_swapUsdjToTrx_addUSDJ {

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
  private String SUN_TRX = "TSf31LHgEdUoid1Wxio58mSqNos8nhKEH9";
  private String USDJ = "TLBaRhANQoJFTqre9Nf1mjuwNWjCJeYqUL";
  private String SUN = "TDqjTkZ63yHB19w2n7vPm2qAkLHwn9fKKk";
  private String WTRX = "TYsbWxNnyTgsZaTFaue9hqpxkU3Fkco94a";
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

  @Test(enabled = true, description = "getInfo swapUsdjToTrx_addUSDJ")
  public void getInfoSwapUsdjToTrx_addUSDJ() {
    TransactionExtention transactionExtention;
    Optional<TransactionInfo> info;
    String txid;

    // totalSupply---USDJ_SUN
    System.out.println("-------------------------lpTotalSupply-------------------------");
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(USDJ_SUN),
            "totalSupply()", "", false, 0, 0, "0", 0, testAccountAddress, testAccountKey,
            blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger lpTotalSupplyUSDJ_SUN = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("USDJ_SUN lpTotalSupply : " + lpTotalSupplyUSDJ_SUN);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(SUN_TRX),
            "totalSupply()", "", false, 0, 0, "0", 0, testAccountAddress, testAccountKey,
            blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger lpTotalSupplySUN_TRX = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("SUN_TRX lpTotalSupply : " + lpTotalSupplySUN_TRX);
    System.out.println("-------------------------lpTotalSupply-------------------------");

    // lpBalance-account
    System.out.println("-------------------------lpBalance-------------------------");
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(USDJ_SUN),
            "balanceOf(address)", "\"" + WalletClient.encode58Check(testAccountAddress) + "\"",
            false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger lpBalanceUSDJ_SUN = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("USDJ_SUN lpBalance : " + lpBalanceUSDJ_SUN);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(SUN_TRX),
            "balanceOf(address)", "\"" + WalletClient.encode58Check(testAccountAddress) + "\"",
            false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger lpBalanceSUN_TRX = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("SUN_TRX lpBalance : " + lpBalanceSUN_TRX);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(USDJ_SUN),
            "balanceOf(address)", "\"" + account2 + "\"",
            false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger lpBalanceAccount2USDJ_SUN = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("USDJ_SUN lpBalanceAccount2 : " + lpBalanceAccount2USDJ_SUN);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(SUN_TRX),
            "balanceOf(address)", "\"" + account2 + "\"",
            false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger lpBalanceAccount2SUN_TRX = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("SUN_TRX lpBalanceAccount2 : " + lpBalanceAccount2SUN_TRX);

    // lpBalance-feeto
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(USDJ_SUN),
            "balanceOf(address)", "\"" + feeTo + "\"",
            false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger lpBalanceFeetoUSDJ_SUN = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("USDJ_SUN lpBalanceFeeto : " + lpBalanceFeetoUSDJ_SUN);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(SUN_TRX),
            "balanceOf(address)", "\"" + feeTo + "\"",
            false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger lpBalanceFeetoSUN_TRX = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("SUN_TRX lpBalanceFeeto : " + lpBalanceFeetoSUN_TRX);
    System.out.println("-------------------------lpBalance-------------------------");

    // tokenbalance-USDJ_SUN
    System.out.println("-------------------------tokenbalance-------------------------");
    String[] tokens = {SUN, USDJ, WTRX};
    String[] accounts = {USDJ_SUN, SUN_TRX, WalletClient.encode58Check(testAccountAddress)};
    Map<String, String> tokensList = new HashMap<>();
    tokensList.put("SUN", SUN);
    tokensList.put("USDJ", USDJ);
    tokensList.put("WTRX", WTRX);
    Map<String, String> accountsList = new HashMap<>();
    tokensList.put("USDJ_SUN", USDJ_SUN);
    tokensList.put("SUN_TRX", SUN_TRX);
    tokensList.put("testAccount1", WalletClient.encode58Check(testAccountAddress));
    Map<String, BigInteger> SUNbalanceList = new HashMap<>();
    /*for
    for (int i = 0; i < tokens.length; i++) {
      for (int j = 0; j < accounts.length; j++) {
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(tokens[i]),
                "balanceOf(address)", "\"" + accounts[j] + "\"",
                false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        BigInteger balance_USDJ_SUN = new BigInteger(ByteArray
            .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
        System.out.println("SUNbalance_USDJ_SUN : " + SUNbalance_USDJ_SUN);

      }
    }*/
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(SUN),
            "balanceOf(address)", "\"" + USDJ_SUN + "\"",
            false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger SUNbalance_USDJ_SUN = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("SUNbalance_USDJ_SUN : " + SUNbalance_USDJ_SUN);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(SUN),
            "balanceOf(address)", "\"" + SUN_TRX + "\"",
            false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger SUNbalance_SUN_TRX = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("SUNbalance_SUN_TRX : " + SUNbalance_SUN_TRX);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(USDJ),
            "balanceOf(address)", "\"" + USDJ_SUN + "\"",
            false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger USDJbalance_USDJ_SUN = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("USDJbalance_USDJ_SUN : " + USDJbalance_USDJ_SUN);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(USDJ),
            "balanceOf(address)", "\"" + SUN_TRX + "\"",
            false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger USDJbalance_SUN_TRX = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("USDJbalance_SUN_TRX : " + USDJbalance_SUN_TRX);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(WTRX),
            "balanceOf(address)", "\"" + USDJ_SUN + "\"",
            false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger WTRXbalance_USDJ_SUN = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("WTRXbalance_USDJ_SUN : " + WTRXbalance_USDJ_SUN);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(WTRX),
            "balanceOf(address)", "\"" + SUN_TRX + "\"",
            false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger WTRXbalance_SUN_TRX = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("WTRXbalance_SUN_TRX : " + WTRXbalance_SUN_TRX);
//    System.out.println("new math.sqrt(kLast) : " + Math.sqrt(tokenbalance1 * tokenbalance2)
//        + "\n-------------------------");

    // tokenbalance-account
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(SUN),
            "balanceOf(address)", "\"" + WalletClient.encode58Check(testAccountAddress) + "\"",
            false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger tokenbalanceA_account1 = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("tokenbalanceA_account1 : " + tokenbalanceA_account1);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(USDJ),
            "balanceOf(address)", "\"" + WalletClient.encode58Check(testAccountAddress) + "\"",
            false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger tokenbalanceB_account1 = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println(
        "tokenbalanceB_account1 : " + tokenbalanceB_account1);
    System.out.println("-------------------------tokenbalance-------------------------");

    System.out.println("---------------------------kLast & getReserves--------------------------");
    // kLast---USDJ_SUN
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(USDJ_SUN),
            "kLast()", "", false, 0, 0, "0", 0, testAccountAddress, testAccountKey,
            blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger kLast = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("kLast : " + kLast);

    // getReserves
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(USDJ_SUN),
            "getReserves()", "", false, 0, 0, "0", 0, testAccountAddress, testAccountKey,
            blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger reserve0 = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray())
        .substring(0, 64), 16);
    System.out.println("reserve0 : " + reserve0);
    BigInteger reserve1 = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray())
        .substring(65, 128), 16);
    System.out.println("reserve1 : " + reserve1);
    BigInteger blockTimestampLast = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray())
        .substring(129, 192), 16);
    System.out
        .println("blockTimestampLast : " + blockTimestampLast);
    System.out.println("---------------------------kLast & getReserves--------------------------");

    // swapExactTokensForTokens
    String[] path = {SUN, USDJ};
    BigInteger swapInAmount = new BigInteger("8000000000000000000");
    txid = swapExactTokensForTokens(swapInAmount, new BigInteger("100000000000"), path,
        WalletClient.encode58Check(testAccountAddress));
    info = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    BigInteger subAmountSUNForSwap = new BigInteger(
        ByteArray.toHexString(info.get().getContractResult(0).toByteArray()).substring(128, 192),
        16);
    BigInteger addAmountUSDJForSwap = new BigInteger(
        ByteArray.toHexString(info.get().getContractResult(0).toByteArray()).substring(192, 256),
        16);
    Assert.assertEquals(swapInAmount, subAmountSUNForSwap);
    System.out.println(
        "subAmountSUNForSwap:" + subAmountSUNForSwap + "   addAmountUSDJForSwap:"
            + addAmountUSDJForSwap);

    // kLast---USDJ_SUN-after
    System.out.println("-------------------------kLast & getReserves-------------------------");
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(USDJ_SUN),
            "kLast()", "", false, 0, 0, "0", 0, testAccountAddress, testAccountKey,
            blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger kLastMid = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("kLastMid : " + kLastMid);

    // getReserves-USDJ_SUN-after
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(USDJ_SUN),
            "getReserves()", "", false, 0, 0, "0", 0, testAccountAddress, testAccountKey,
            blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger reserve0Mid = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray())
        .substring(0, 64), 16);
    System.out.println("reserve0Mid : " + reserve0Mid);
    BigInteger reserve1Mid = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray())
        .substring(65, 128), 16);
    System.out.println("reserve1Mid : " + reserve1Mid);
    Assert.assertEquals(reserve0.add(swapInAmount),
        reserve0Mid);
    Assert.assertEquals(reserve1.subtract(addAmountUSDJForSwap),
        reserve1Mid);
    System.out.println("-------------------------kLast & getReserves-------------------------");

    // removeLiquidity
    txid = removeLiquidity(SUN, USDJ, new BigInteger("2000000"), new BigInteger("1"),
        new BigInteger("1"),
        WalletClient.encode58Check(testAccountAddress));
    info = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    BigInteger addAmountAForRemoveLp = new BigInteger(
        ByteArray.toHexString(info.get().getContractResult(0).toByteArray()).substring(0, 64), 16);
    BigInteger addAmountBForRemoveLp = new BigInteger(
        ByteArray.toHexString(info.get().getContractResult(0).toByteArray()).substring(64, 128),
        16);
    System.out.println(
        "addAmountAForRemoveLp:" + addAmountAForRemoveLp + "   addAmountBForRemoveLp:"
            + addAmountBForRemoveLp);

    // totalSupply---USDJ_SUN-after
    System.out.println("-------------------------lpTotalSupply-------------------------");
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(USDJ_SUN),
            "totalSupply()", "", false, 0, 0, "0", 0, testAccountAddress, testAccountKey,
            blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger lpTotalSupplyAfter = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out
        .println("lpTotalSupplyAfter : " + lpTotalSupplyAfter);
    System.out.println("-------------------------lpTotalSupply-------------------------");

    // lpBalance-account-after
    System.out.println("-------------------------lpBalance-------------------------");
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(USDJ_SUN),
            "balanceOf(address)", "\"" + WalletClient.encode58Check(testAccountAddress) + "\"",
            false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger lpBalanceAfter = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("lpBalanceAfter : " + lpBalanceAfter);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(USDJ_SUN),
            "balanceOf(address)", "\"" + account2 + "\"",
            false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger lpBalanceAccount2After = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out
        .println("lpBalanceAccount2After : " + lpBalanceAccount2After);
    Assert.assertEquals(lpBalanceUSDJ_SUN.subtract(new BigInteger("2000000")), lpBalanceAfter);

    // lpbalance-feeTo-after
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(USDJ_SUN),
            "balanceOf(address)", "\"" + feeTo + "\"",
            false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger lpBalanceFeetoAfter = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("lpBalanceFeetoAfter : " + lpBalanceFeetoAfter);
    Assert.assertEquals(lpBalanceFeetoAfter.add(lpBalanceAfter).add(lpBalanceAccount2After),
        lpTotalSupplyAfter.subtract(new BigInteger("1000")));
    System.out.println("-------------------------lpBalance-------------------------");
    BigInteger fee = lpBalanceFeetoAfter.subtract(lpBalanceFeetoUSDJ_SUN);
    System.out.println("fee: " + fee);
    Assert.assertEquals(lpTotalSupplyUSDJ_SUN.subtract(new BigInteger("2000000")).add(fee),
        lpTotalSupplyAfter);

    // tokenbalance---USDJ_SUN-after
    System.out.println("-------------------------tokenbalance-------------------------");
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(SUN),
            "balanceOf(address)", "\"" + USDJ_SUN + "\"",
            false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger tokenbalanceA_USDJ_SUN_After = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("tokenbalanceA_USDJ_SUN_After : " + tokenbalanceA_USDJ_SUN_After);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(USDJ),
            "balanceOf(address)", "\"" + USDJ_SUN + "\"",
            false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger tokenbalanceB_USDJ_SUN_After = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("tokenbalanceB_USDJ_SUN_After : " + tokenbalanceB_USDJ_SUN_After + "\n");
    Assert
        .assertEquals(
            SUNbalance_USDJ_SUN.add(swapInAmount).subtract(addAmountAForRemoveLp),
            tokenbalanceA_USDJ_SUN_After);
    Assert.assertEquals(
        USDJbalance_USDJ_SUN.subtract(addAmountUSDJForSwap).subtract(addAmountBForRemoveLp),
        tokenbalanceB_USDJ_SUN_After);
//    System.out.println("new math.sqrt(kLast) : " + Math.sqrt(tokenbalance1 * tokenbalance2)
//        + "\n-------------------------");

    // tokenbalance-account-after
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(SUN),
            "balanceOf(address)", "\"" + WalletClient.encode58Check(testAccountAddress) + "\"",
            false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger tokenbalanceA_account1_after = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("tokenbalanceA_account1_after : " + tokenbalanceA_account1_after);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(USDJ),
            "balanceOf(address)", "\"" + WalletClient.encode58Check(testAccountAddress) + "\"",
            false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger tokenbalanceB_account1_after = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("tokenbalanceB_account1_after : " + tokenbalanceB_account1_after);
    Assert
        .assertEquals(tokenbalanceA_account1.subtract(swapInAmount).add(addAmountAForRemoveLp),
            tokenbalanceA_account1_after);
    Assert.assertEquals(
        tokenbalanceB_account1.add(addAmountUSDJForSwap).add(addAmountBForRemoveLp),
        tokenbalanceB_account1_after);
    System.out.println("-------------------------tokenbalance-------------------------");

    // kLast---USDJ_SUN-after
    System.out.println("-------------------------kLast & getReserves-------------------------");
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(USDJ_SUN),
            "kLast()", "", false, 0, 0, "0", 0, testAccountAddress, testAccountKey,
            blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger kLastAfter = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
    System.out.println("kLastAfter : " + kLastAfter);

    // getReserves-USDJ_SUN-after
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(USDJ_SUN),
            "getReserves()", "", false, 0, 0, "0", 0, testAccountAddress, testAccountKey,
            blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    BigInteger reserve0After = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray())
        .substring(0, 64), 16);
    System.out.println("reserve0After : " + reserve0After);
    BigInteger reserve1After = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray())
        .substring(65, 128), 16);
    System.out.println("reserve1After : " + reserve1After);
    BigInteger blockTimestampLastAfter = new BigInteger(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray())
        .substring(129, 192), 16);
    System.out
        .println("blockTimestampLastAfter : " + blockTimestampLastAfter);
    Assert.assertEquals(reserve0.add(swapInAmount).subtract(addAmountAForRemoveLp),
        reserve0After);
    Assert.assertEquals(reserve1.subtract(addAmountUSDJForSwap).subtract(addAmountBForRemoveLp),
        reserve1After);
    System.out.println("-------------------------kLast & getReserves-------------------------");
  }

  @Test(enabled = false)
  public void test() {
    // swapExactTokensForTokens
    String[] path = {USDJ, SUN};
    String txid = swapExactTokensForTokens(new BigInteger("2000000000000000000"),
        new BigInteger("100000000000000000"), path,
        WalletClient.encode58Check(testAccountAddress));

  }

  public String swapExactTokensForTokens(BigInteger amountIn, BigInteger amountOut, String[] path,
      String to) {
    String txid;
    String param;
    Optional<TransactionInfo> infoById;
    String paths = "[";
    for (int i = 0; i < path.length - 1; i++) {
      paths += "\"" + path[i] + "\",";
    }
    paths += "\"" + path[path.length - 1] + "\"]";
    param =
        amountIn + "," + amountOut + "," + paths + ",\"" + to + "\"," + getTimestamp("2021-12-31");
    param = PublicMethed.addZeroForNum(amountIn.toString(16), 64)
        + PublicMethed.addZeroForNum(amountOut.toString(16), 64)
        + PublicMethed.addZeroForNum(Integer.toHexString(160), 64)
        + PublicMethed
        .addZeroForNum(ByteArray.toHexString(WalletClient.decodeFromBase58Check(to)), 64)
        + PublicMethed.addZeroForNum(Integer.toHexString(getTimestamp("2021-12-31")), 64)
        + PublicMethed.addZeroForNum(Integer.toHexString(path.length), 64);
    for (int i = 0; i < path.length; i++) {
      param += PublicMethed
          .addZeroForNum(ByteArray.toHexString(WalletClient.decodeFromBase58Check(path[i])), 64);
    }
    System.out.println("param: " + param);
    txid = PublicMethed
        .triggerContract(WalletClient.decodeFromBase58Check(UniswapV2Router02),
            "swapExactTokensForTokens(uint256,uint256,address[],address,uint256)", param, true,
            0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
    return txid;
  }

  public String removeLiquidity(String tokenA, String tokenB, BigInteger liquidity,
      BigInteger amountAMin,
      BigInteger amountBMin, String to) {
    String txid;
    String param;
    Optional<TransactionInfo> infoById;
    param =
        "\"" + tokenA + "\",\"" + tokenB + "\"," + liquidity + "," + amountAMin + "," + amountBMin
            + ",\"" + to + "\"," + getTimestamp("2021-12-31");
    txid = PublicMethed
        .triggerContract(WalletClient.decodeFromBase58Check(UniswapV2Router02),
            "removeLiquidity(address,address,uint256,uint256,uint256,address,uint256)",
            param, false,
            0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
    return txid;
  }
  /*@Test(enabled = true, description = "get3PoolLPAndApproveLPStaker")
  public void get3PoolLPAndApproveLPStaker() {
    String txid;
    String param;
    Optional<TransactionInfo> infoById;
    TransactionExtention transactionExtention;
    // USDJ approve to 3pool
    param = "\"" + threePoolAddress + "\",-1";
    txid = PublicMethed
        .triggerContract(WalletClient.decodeFromBase58Check(USDJAddr), "approve(address,uint256)",
            param, false,
            0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // TUSD approve to 3pool
    txid = PublicMethed
        .triggerContract(WalletClient.decodeFromBase58Check(TUSDAddr), "approve(address,uint256)",
            param, false,
            0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // USDT approve to 3pool
    txid = PublicMethed
        .triggerContract(WalletClient.decodeFromBase58Check(USDTAddr), "approve(address,uint256)",
            param, false,
            0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // add_liquidity
    param = "[\"3476461800046232653\",\"3344752975453815606\",\"3476453\"],0";
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(threePoolAddress),
        "add_liquidity(uint256[3],uint256)", param, false,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
    // get 3pool lp count
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(threePool_lp),
            "balanceOf(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"", false,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    String balancesHex = ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray())
        .replaceAll("^(0+)", "");
    balancesHex = balancesHex.length() == 0 ? "0" : balancesHex;
    Long balances = Long.parseLong(balancesHex, 16);
    System.out.println("3pool lp balances : " + balances);

    param = "\"" + LpTokenStakerAuto + "\",-1";
    // 3ppol lp approve to LpTokenStakerAuto
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(threePool_lp),
        "approve(address,uint256)", param, false,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // sun-trx lp approve to LpTokenStakerAuto
    txid = PublicMethed
        .triggerContract(WalletClient.decodeFromBase58Check(sun_trx_lp), "approve(address,uint256)",
            param, false,
            0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // usdc_3sun_lp approve to LpTokenStakerAuto
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(usdc_3sun_lp),
        "approve(address,uint256)", param, false,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
  }

  @Test(enabled = true, description = "getUsdc3SUNPoolLPAndApproveLPStaker")
  public void getUsdc3SUNPoolLPAndApproveLPStaker() {
    String txid;
    String param;
    Optional<TransactionInfo> infoById;
    TransactionExtention transactionExtention;
    // USDJ approve to USDCDepositer
    param = "\"" + USDCDepositer + "\",-1";
    txid = PublicMethed
        .triggerContract(WalletClient.decodeFromBase58Check(USDJAddr), "approve(address,uint256)",
            param, false,
            0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // TUSD approve to USDCDepositer
    txid = PublicMethed
        .triggerContract(WalletClient.decodeFromBase58Check(TUSDAddr), "approve(address,uint256)",
            param, false,
            0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // USDT approve to USDCDepositer
    txid = PublicMethed
        .triggerContract(WalletClient.decodeFromBase58Check(USDTAddr), "approve(address,uint256)",
            param, false,
            0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // USDC approve to USDCDepositer
    txid = PublicMethed
        .triggerContract(WalletClient.decodeFromBase58Check(USDCAddr), "approve(address,uint256)",
            param, false,
            0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // add_liquidity
    String num1 = PublicMethed.addZeroForNum(new BigInteger("3576453").toString(16), 64);
    String num2 = PublicMethed
        .addZeroForNum(new BigInteger("3344752975453815606").toString(16), 64);
    String num3 = PublicMethed
        .addZeroForNum(new BigInteger("3476461800046232653").toString(16), 64);
    String num4 = PublicMethed.addZeroForNum(new BigInteger("3476453").toString(16), 64);
    String num5 = PublicMethed.addZeroForNum(new BigInteger("0").toString(16), 64);
    param = num1 + num2 + num3 + num4 + num5;
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(USDCDepositer),
        "add_liquidity(uint256[4],uint256)", param, true,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
    // get usdc3sun lp count
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(usdc_3sun_lp),
            "balanceOf(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"", false,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    String balancesHex = ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray())
        .replaceAll("^(0+)", "");
    balancesHex = balancesHex.length() == 0 ? "0" : balancesHex;
    balancesHex = new BigInteger(balancesHex, 16).toString(10);
    System.out.println("usdc3sun lp balances : " + balancesHex);

    // usdc3sun lp approve to threePoolLpGauge
    param = "\"" + threePoolLpGauge + "\",-1";
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(threePool_lp),
        "approve(address,uint256)", param, false,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // sun-trx lp approve to sunTrxPoolLpGauge
    param = "\"" + sunTrxPoolLpGauge + "\",-1";
    txid = PublicMethed
        .triggerContract(WalletClient.decodeFromBase58Check(sun_trx_lp), "approve(address,uint256)",
            param, false,
            0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // usdc_3sun_lp approve to usdc3SUNPoolLpGauge
    param = "\"" + usdc3SUNPoolLpGauge + "\",-1";
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(usdc_3sun_lp),
        "approve(address,uint256)", param, false,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
  }

  @Test(enabled = true, description = "usdc3SUNPoolLP_deposit")
  public void usdc3SUNPoolLP_deposit() {
    String txid;
    String param;
    Optional<TransactionInfo> infoById;
    TransactionExtention transactionExtention;
    // usdc_3sun_lp approve to usdc3SUNPoolLpGauge
    param = "\"" + usdc3SUNPoolLpGauge + "\",-1";
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(usdc_3sun_lp), "approve(address,uint256)", param, false,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // deposit
    String num1 = PublicMethed
        .addZeroForNum(new BigInteger("1000000000000000000").toString(16), 64);
    param = num1;
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(usdc3SUNPoolLpGauge),
        "deposit(uint256)", param, true,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // withdraw
    num1 = PublicMethed.addZeroForNum(new BigInteger("1000000000000000000").toString(16),64);
    param = num1;
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(usdc3SUNPoolLpGauge), "withdraw(uint256)", param, true,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // get usdc3sun deposit count
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(
            WalletClient.decodeFromBase58Check(usdc3SUNPoolLpGauge),
            "balanceOf(address)",
            PublicMethed.addZeroForNum(ByteArray.toHexString(fromAddress).substring(2), 64), true,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    String balancesHex = ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray())
        .replaceAll("^(0+)", "");
    balancesHex = balancesHex.length() == 0 ? "0" : balancesHex;
    balancesHex = new BigInteger(balancesHex, 16).toString(10);
    System.out.println("usdc3sun deposit balances : " + balancesHex);
  }

  @Test(enabled = true, description = "veSUNStaker_stakeWithLock")
  public void veSUNStaker_stakeWithLock() {
    TransactionExtention transactionExtention;
    // lockedBalances
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
            "lockedBalances(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"", false,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    String lockedBalancesBeforeHex = ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()).substring(128, 192)
        .replaceAll("^(0+)", "");
    lockedBalancesBeforeHex = lockedBalancesBeforeHex.length() == 0 ? "0" : lockedBalancesBeforeHex;
    Long lockedBalancesBefore = Long.parseLong(lockedBalancesBeforeHex, 16);
    System.out.println("lockedBalancesBefore: " + lockedBalancesBefore);
    String lockEndTimestampHex = ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()).substring(256)
        .replaceAll("^(0+)", "");
    lockEndTimestampHex = lockEndTimestampHex.length() == 0 ? "0" : lockEndTimestampHex;
    int lockEndTimestamp = Integer.parseInt(lockEndTimestampHex, 16);
    System.out.println("lockEndTimestamp: " + lockEndTimestamp);
    // lockedSupply
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
            "lockedSupply()", "", false,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Long lockedSupplyBefore = ByteArray
        .toLong(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("lockedSupplyBefore: " + lockedSupplyBefore);
    // totalBalance
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
            "totalBalance(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"", false,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Long totalBalanceBefore = ByteArray
        .toLong(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("totalBalanceBefore: " + totalBalanceBefore);
    // totalSupply
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
            "totalSupply()", "", false,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Long totalSupplyBefore = ByteArray
        .toLong(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("totalSupplyBefore: " + totalSupplyBefore);
    // unlockedBalance
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
            "unlockedBalance(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"",
            false,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Long unlockedBalanceBefore = ByteArray
        .toLong(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("unlockedBalanceBefore: " + unlockedBalanceBefore);

    int currentTimestamp = getSecondTimestampTwo(0);
    System.out.println("currentTimestamp: " + currentTimestamp);
    System.out.println("is end: " + (currentTimestamp > lockEndTimestamp));

    String txid;
    Optional<TransactionInfo> infoById;
    boolean needStake = true;
    if (needStake) {
      // get allowance
      transactionExtention = PublicMethed
          .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(sspSUNAddr),
              "allowance(address,address)",
              "\"" + WalletClient.encode58Check(fromAddress) + "\",\"" + veSunStaker + "\"", false,
              0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      String allowanceHex = ByteArray
          .toHexString(transactionExtention.getConstantResult(0).toByteArray())
          .replaceAll("^(0+)", "");
      allowanceHex = allowanceHex.length() == 0 ? "0" : allowanceHex;
      allowanceHex = allowanceHex.contains("ffffffffffffffffffffffffffffff") ? "-1" : allowanceHex;
      Long allowance = Long.parseLong(allowanceHex, 16);
      if (allowance == 0) {
        // approve
        txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(sspSUNAddr),
            "approve(address,uint256)", "\"" + veSunStaker + "\",-1", false,
            0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
        Assert.assertEquals(0, infoById.get().getResultValue());
      }

      if (lockedBalancesBefore == 0) {
        if (lockEndTimestamp > 0 && currentTimestamp >= lockEndTimestamp) {
          // withdrawExpiredLocks
          txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(veSunStaker),
              "withdrawExpiredLocks()", "", false,
              0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
          PublicMethed.waitProduceNextBlock(blockingStubFull);
          infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
          Assert.assertEquals(0, infoById.get().getResultValue());

          transactionExtention = PublicMethed
              .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
                  "lockedSupply()", "", false,
                  0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
          Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
          lockedSupplyBefore = ByteArray
              .toLong(transactionExtention.getConstantResult(0).toByteArray());
          System.out.println("lockedSupplyBefore: " + lockedSupplyBefore);
        }
        // stakeWithLock
        txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(veSunStaker),
            "stake(uint256,bool,uint256)",
            "123456781234567890,true," + getSecondTimestampTwo(3600), false,
            0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
        Assert.assertEquals(0, infoById.get().getResultValue());

      } else if (lockedBalancesBefore > 0) {
        // increaseLock
        int lockTime = getSecondTimestampTwo(3600);
        txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(veSunStaker),
            "increaseLock(uint256,uint256)", "123456781234567890," + lockTime, false,
            0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
        PublicMethed.waitProduceNextBlock(blockingStubFull);
        infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
        Assert.assertEquals(0, infoById.get().getResultValue());
      }

      if (lockedBalancesBefore == 0) {
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
                "lockedBalances(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"",
                false,
                0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        String lockedBalancesAfterHex = ByteArray
            .toHexString(transactionExtention.getConstantResult(0).toByteArray())
            .substring(128, 192).replaceAll("^(0+)", "");
        lockedBalancesAfterHex =
            lockedBalancesAfterHex.length() == 0 ? "0" : lockedBalancesAfterHex;
        Long lockedBalancesAfter = Long.parseLong(lockedBalancesAfterHex, 16);
        System.out.println(
            "lockedBalancesBefore: " + lockedBalancesBefore + ",lockedBalancesAfter: "
                + lockedBalancesAfter);
        String lockEndTimestampAfterHex = ByteArray
            .toHexString(transactionExtention.getConstantResult(0).toByteArray()).substring(256)
            .replaceAll("^(0+)", "");
        lockEndTimestampAfterHex =
            lockEndTimestampAfterHex.length() == 0 ? "0" : lockEndTimestampAfterHex;
        int lockEndTimestampAfter = Integer.parseInt(lockEndTimestampAfterHex, 16);
        System.out.println("lockEndTimestampAfter: " + lockEndTimestampAfter);
        Assert
            .assertEquals(lockedBalancesBefore + 123456781234567890l,
                lockedBalancesAfter.longValue());
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
                "lockedSupply()", "", false,
                0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        Long lockedSupplyAfter = ByteArray
            .toLong(transactionExtention.getConstantResult(0).toByteArray());
        System.out.println("lockedSupplyBefore: " + lockedSupplyBefore + ",lockedSupplyAfter: "
            + lockedSupplyAfter);
        Assert
            .assertEquals(lockedSupplyBefore + 123456781234567890l, lockedSupplyAfter.longValue());
        // totalBalance
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
                "totalBalance(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"",
                false,
                0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        Long totalBalanceAfter = ByteArray
            .toLong(transactionExtention.getConstantResult(0).toByteArray());
        System.out.println("totalBalanceBefore: " + totalBalanceBefore + ",totalBalanceAfter: "
            + totalBalanceAfter);
        Assert
            .assertEquals(totalBalanceBefore + 123456781234567890l, totalBalanceAfter.longValue());
        // totalSupply
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
                "totalSupply()", "", false,
                0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        Long totalSupplyAfter = ByteArray
            .toLong(transactionExtention.getConstantResult(0).toByteArray());
        System.out.println(
            "totalSupplyBefore: " + totalSupplyBefore + ",totalSupplyAfter: " + totalSupplyAfter);
        Assert
            .assertEquals(totalSupplyBefore + 123456781234567890l, totalSupplyAfter.longValue());
        // unlockedBalance
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
                "unlockedBalance(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"",
                false,
                0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        Long unlockedBalanceAfter = ByteArray
            .toLong(transactionExtention.getConstantResult(0).toByteArray());
        System.out.println(
            "unlockedBalanceBefore: " + unlockedBalanceBefore + ",unlockedBalanceAfter: "
                + unlockedBalanceAfter);
        Assert
            .assertEquals(unlockedBalanceBefore.longValue(), unlockedBalanceAfter.longValue());
      } else {
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
                "lockedBalances(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"",
                false,
                0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        String lockedBalancesAfterHex = ByteArray
            .toHexString(transactionExtention.getConstantResult(0).toByteArray())
            .substring(128, 192).replaceAll("^(0+)", "");
        lockedBalancesAfterHex =
            lockedBalancesAfterHex.length() == 0 ? "0" : lockedBalancesAfterHex;
        Long lockedBalancesAfter = Long.parseLong(lockedBalancesAfterHex, 16);
        System.out.println(
            "lockedBalancesBefore: " + lockedBalancesBefore + ",lockedBalancesAfter: "
                + lockedBalancesAfter);
        String lockEndTimestampAfterHex = ByteArray
            .toHexString(transactionExtention.getConstantResult(0).toByteArray()).substring(256)
            .replaceAll("^(0+)", "");
        lockEndTimestampAfterHex =
            lockEndTimestampAfterHex.length() == 0 ? "0" : lockEndTimestampAfterHex;
        int lockEndTimestampAfter = Integer.parseInt(lockEndTimestampAfterHex, 16);
        System.out.println("lockEndTimestampAfter: " + lockEndTimestampAfter);
        Assert
            .assertEquals(lockedBalancesBefore.longValue(), lockedBalancesAfter.longValue());
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
                "lockedSupply()", "", false,
                0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        Long lockedSupplyAfter = ByteArray
            .toLong(transactionExtention.getConstantResult(0).toByteArray());
        System.out.println("lockedSupplyBefore: " + lockedSupplyBefore + ",lockedSupplyAfter: "
            + lockedSupplyAfter);
        Assert
            .assertEquals(lockedSupplyBefore.longValue(), lockedSupplyAfter.longValue());
        // totalBalance
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
                "totalBalance(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"",
                false,
                0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        Long totalBalanceAfter = ByteArray
            .toLong(transactionExtention.getConstantResult(0).toByteArray());
        System.out.println("totalBalanceBefore: " + totalBalanceBefore + ",totalBalanceAfter: "
            + totalBalanceAfter);
        Assert
            .assertEquals(totalBalanceBefore.longValue(), totalBalanceAfter.longValue());
        // totalSupply
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
                "totalSupply()", "", false,
                0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        Long totalSupplyAfter = ByteArray
            .toLong(transactionExtention.getConstantResult(0).toByteArray());
        System.out.println(
            "totalSupplyBefore: " + totalSupplyBefore + ",totalSupplyAfter: " + totalSupplyAfter);
        Assert
            .assertEquals(totalSupplyBefore.longValue(), totalSupplyAfter.longValue());
        // unlockedBalance
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSunStaker),
                "unlockedBalance(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"",
                false,
                0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        Long unlockedBalanceAfter = ByteArray
            .toLong(transactionExtention.getConstantResult(0).toByteArray());
        System.out.println(
            "unlockedBalanceBefore: " + unlockedBalanceBefore + ",unlockedBalanceAfter: "
                + unlockedBalanceAfter);
        Assert
            .assertEquals(unlockedBalanceBefore.longValue(), unlockedBalanceAfter.longValue());
      }
    }
    // veSUN.balanceOf
    String param = PublicMethed
        .addZeroForNum(ByteArray.toHexString(fromAddress).substring(2), 64);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(veSUN),
            "balanceOf(bytes32)", param, true,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    String balanceOfHex = ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray())
        .replaceAll("^(0+)", "");
    balanceOfHex = balanceOfHex.length() == 0 ? "0" : balanceOfHex;
    Long balanceOf = Long.parseLong(balanceOfHex, 16);
    System.out.println("veSUN.balanceOf: " + balanceOf.longValue());
  }

  @Test(enabled = true, description = "vote_vote_for_gauge_weights")
  public void vote_vote_for_gauge_weights() {
    TransactionExtention transactionExtention;
    String txid;
    Optional<TransactionInfo> infoById;
    // vote_for_gauge_weights
    String param = PublicMethed.addZeroForNum(
        ByteArray.toHexString(WalletClient.decodeFromBase58Check(threePool_lp)).substring(2), 64)
        + PublicMethed.addZeroForNum(Integer.toHexString(1600), 64);
    System.out.println("param: " + param);
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(Vote),
        "vote_for_gauge_weights(bytes32,uint256)", param, true,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // gauge_relative_weight
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    param = PublicMethed.addZeroForNum(
        ByteArray.toHexString(WalletClient.decodeFromBase58Check(threePool_lp)).substring(2), 64)
        + PublicMethed
        .addZeroForNum(Integer.toHexString(getNextHalfAHourTimestamp().intValue()), 64);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(Vote),
            "gauge_relative_weight(bytes32,uint256)", param, true,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    System.out.println("gauge_relative_weight: " + ByteArray
        .toLong(transactionExtention.getConstantResult(0).toByteArray()));

    // vote_for_gauge_weights
    param = PublicMethed.addZeroForNum(
        ByteArray.toHexString(WalletClient.decodeFromBase58Check(sun_trx_lp)).substring(2), 64)
        + PublicMethed.addZeroForNum(Integer.toHexString(1600), 64);
    System.out.println("param: " + param);
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(Vote),
        "vote_for_gauge_weights(bytes32,uint256)", param, true,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // gauge_relative_weight
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    param = PublicMethed.addZeroForNum(
        ByteArray.toHexString(WalletClient.decodeFromBase58Check(sun_trx_lp)).substring(2), 64)
        + PublicMethed
        .addZeroForNum(Integer.toHexString(getNextHalfAHourTimestamp().intValue()), 64);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(Vote),
            "gauge_relative_weight(bytes32)", param, true,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    System.out.println("gauge_relative_weight: " + ByteArray
        .toLong(transactionExtention.getConstantResult(0).toByteArray()));

    // vote_for_gauge_weights
    param = PublicMethed.addZeroForNum(
        ByteArray.toHexString(WalletClient.decodeFromBase58Check(usdc_3sun_lp)).substring(2), 64)
        + PublicMethed.addZeroForNum(Integer.toHexString(6800), 64);
    System.out.println("param: " + param);
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(Vote),
        "vote_for_gauge_weights(bytes32,uint256)", param, true,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // gauge_relative_weight
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    param = PublicMethed.addZeroForNum(
        ByteArray.toHexString(WalletClient.decodeFromBase58Check(usdc_3sun_lp)).substring(2), 64)
        + PublicMethed
        .addZeroForNum(Integer.toHexString(getNextHalfAHourTimestamp().intValue()), 64);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(Vote),
            "gauge_relative_weight(bytes32)", param, true,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    System.out.println("gauge_relative_weight: " + ByteArray
        .toLong(transactionExtention.getConstantResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "getEndLockTime")
  public void getEndLockTime() {
    TransactionExtention transactionExtention;
    String txid;
    Optional<TransactionInfo> infoById;
    // vote_for_gauge_weights
    String param = PublicMethed.addZeroForNum(
        ByteArray.toHexString(WalletClient.decodeFromBase58Check(threePool_lp)).substring(2), 64)
        + PublicMethed.addZeroForNum(Integer.toHexString(3000), 64);
    System.out.println("param: " + param);
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(Vote),
        "vote_for_gauge_weights(bytes32,uint256)", param, true,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    // gauge_relative_weight
    param = PublicMethed.addZeroForNum(
        ByteArray.toHexString(WalletClient.decodeFromBase58Check(threePool_lp)).substring(2), 64)
        + PublicMethed
        .addZeroForNum(Integer.toHexString(getNextHalfAHourTimestamp().intValue()), 64);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(Vote),
            "gauge_relative_weight(bytes32,uint256)", param, true,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    System.out.println("gauge_relative_weight: " + ByteArray
        .toLong(transactionExtention.getConstantResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "vote_gauge_relative_weight_write")
  public void vote_gauge_relative_weight_write() {
    String txid;
    Optional<TransactionInfo> infoById;
    // gauge_relative_weight_write
    String param = PublicMethed.addZeroForNum(
        ByteArray.toHexString(WalletClient.decodeFromBase58Check(threePool_lp)).substring(2), 64);
    System.out.println("param: " + param);
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(Vote),
        "gauge_relative_weight_write(bytes32)", param, true,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    param = PublicMethed.addZeroForNum(
        ByteArray.toHexString(WalletClient.decodeFromBase58Check(sun_trx_lp)).substring(2), 64);
    System.out.println("param: " + param);
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(Vote),
        "gauge_relative_weight_write(bytes32)", param, true,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());

    param = PublicMethed.addZeroForNum(
        ByteArray.toHexString(WalletClient.decodeFromBase58Check(usdc_3sun_lp)).substring(2), 64);
    System.out.println("param: " + param);
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(Vote),
        "gauge_relative_weight_write(bytes32)", param, true,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
  }



  @Test(enabled = true, description = "removeAndAddLiquidity")
  public void removeAndAddLiquidity() {
    String txid;
    String param;
    Optional<TransactionInfo> infoById;
    TransactionExtention transactionExtention;
    // threePool_lp balanceOf
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(threePool_lp),
            "balanceOf(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"", false,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    String balanceBeforeHex = ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("\n3pool lp balance before:" + balanceBeforeHex);
    if (!balanceBeforeHex
        .equals("0000000000000000000000000000000000000000000000000000000000000000")) {
      // threePool remove_liquidity all balance
      param = balanceBeforeHex
          + "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
      logger.info("param:" + param);
      txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(threePoolAddress),
          "remove_liquidity(uint256,uint256[3])", param, true,
          0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
      Assert.assertEquals(0, infoById.get().getResultValue());

      transactionExtention = PublicMethed
          .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(threePool_lp),
              "balanceOf(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"", false,
              0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      String balanceAfterHex = ByteArray
          .toHexString(transactionExtention.getConstantResult(0).toByteArray());
      System.out.println("\n3pool lp balance after:" + balanceAfterHex);
    }

    // threePool add_liquidity
    String num1 = PublicMethed
        .addZeroForNum(new BigInteger("12000000000000000000").toString(16), 64);
    String num2 = PublicMethed
        .addZeroForNum(new BigInteger("12000000000000000000").toString(16), 64);
    String num3 = PublicMethed.addZeroForNum(new BigInteger("12000000").toString(16), 64);
    String num4 = PublicMethed.addZeroForNum(new BigInteger("0").toString(16), 64);
    param = num1 + num2 + num3 + num4;
    System.out.println("param:" + param);
    txid = PublicMethed.triggerContract(WalletClient.decodeFromBase58Check(threePoolAddress),
        "add_liquidity(uint256[3],uint256)", param, true,
        0, maxFeeLimit, fromAddress, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    // get 3pool lp count
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(WalletClient.decodeFromBase58Check(threePool_lp),
            "balanceOf(address)", "\"" + WalletClient.encode58Check(fromAddress) + "\"", false,
            0, 0, "0", 0, fromAddress, testKey001, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    String balancesHex = ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray());
    System.out.println("3pool lp balances : " + balancesHex);
  }*/

  public static Integer getTimestamp(String time) {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    Date date = null;
    try {
      date = df.parse(time);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    long timestamp = cal.getTimeInMillis() / 1000;
    return (int) timestamp;
  }

  public static int getSecondTimestampTwo(int addTime) {
    String timestamp = String.valueOf(new Date().getTime() / 1000);
    return Integer.valueOf(timestamp) + addTime;
  }

  public static Long getNextHalfAHourTimestamp() {
    Long l = (System.currentTimeMillis() - System.currentTimeMillis() % 1800000 + 1800000) / 1000;
    System.out.println("nextHalfAHourTimestamp is:" + l);
    System.out.println("date is:" + new Date(l));
    return l;
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
