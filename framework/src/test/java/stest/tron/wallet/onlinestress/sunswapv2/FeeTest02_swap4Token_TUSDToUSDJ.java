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
public class FeeTest02_swap4Token_TUSDToUSDJ {

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
  private String TUSD_TRX = "TZ5BwVXKQNNkjnWC61MvGxPpafvSHbpCkZ";
  private String USDJ = "TLBaRhANQoJFTqre9Nf1mjuwNWjCJeYqUL";
  private String SUN = "TDqjTkZ63yHB19w2n7vPm2qAkLHwn9fKKk";
  private String WTRX = "TYsbWxNnyTgsZaTFaue9hqpxkU3Fkco94a";
  private String TUSD = "TRz7J6dD2QWxBoumfYt4b3FaiRG23pXfop";
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

  @Test(enabled = true, description = "getInfo swap4Token_TUSDToUSDJ")
  public void getInfoSwap4Token_TUSDToUSDJ() {
    TransactionExtention transactionExtention;
    Optional<TransactionInfo> info;
    String txid;
    Map<String, String> tokensList = new HashMap<>();
    tokensList.put("SUN", SUN);
    tokensList.put("USDJ", USDJ);
    tokensList.put("WTRX", WTRX);
    tokensList.put("TUSD", TUSD);
    Map<String, String> checkTokenBalanceAccountsList = new HashMap<>();
    checkTokenBalanceAccountsList.put("USDJ_SUN", USDJ_SUN);
    checkTokenBalanceAccountsList.put("SUN_TRX", SUN_TRX);
    checkTokenBalanceAccountsList.put("TUSD_TRX", TUSD_TRX);
    checkTokenBalanceAccountsList
        .put("account1", WalletClient.encode58Check(testAccountAddress));
    Map<String, String> pairsList = new HashMap<>();
    pairsList.put("USDJ_SUN", USDJ_SUN);
    pairsList.put("SUN_TRX", SUN_TRX);
    pairsList.put("TUSD_TRX", TUSD_TRX);
    Map<String, String> checkLpBalanceAccountsList = new HashMap<>();
    checkLpBalanceAccountsList.put("account1", WalletClient.encode58Check(testAccountAddress));
    checkLpBalanceAccountsList.put("account2", account2);
    checkLpBalanceAccountsList.put("feeTo", feeTo);

    // totalSupply
    System.out.println("-------------------------lpTotalSupply-------------------------");
    Map<String, BigInteger> totalSupplyList = new HashMap<>();
    for (String pair : pairsList.keySet()) {
      transactionExtention = PublicMethed
          .triggerConstantContractForExtention(
              WalletClient.decodeFromBase58Check(pairsList.get(pair)), "totalSupply()", "", false,
              0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      BigInteger lpTotalSupply = new BigInteger(ByteArray
          .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
      System.out.println(pair + "_lpTotalSupply : " + lpTotalSupply);
      totalSupplyList.put(pair + "_lpTotalSupply", lpTotalSupply);
    }
    System.out.println("-------------------------lpTotalSupply-------------------------");

    // lpBalance
    System.out.println("-------------------------lpBalance-------------------------");
    Map<String, BigInteger> lpbalanceList = new HashMap<>();
    for (String pair : pairsList.keySet()) {
      for (String account : checkLpBalanceAccountsList.keySet()) {
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(
                WalletClient.decodeFromBase58Check(pairsList.get(pair)),
                "balanceOf(address)", "\"" + checkLpBalanceAccountsList.get(account) + "\"",
                false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        BigInteger balance = new BigInteger(ByteArray
            .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
        System.out.println(pair + "_Lpbalance_" + account + " : " + balance);
        lpbalanceList.put(pair + "_Lpbalance_" + account, balance);
      }
    }
    System.out.println("-------------------------lpBalance-------------------------");

    // tokenbalance
    System.out.println("-------------------------tokenbalance-------------------------");
    Map<String, BigInteger> tokenbalanceList = new HashMap<>();
    for (String token : tokensList.keySet()) {
      for (String account : checkTokenBalanceAccountsList.keySet()) {
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(
                WalletClient.decodeFromBase58Check(tokensList.get(token)),
                "balanceOf(address)", "\"" + checkTokenBalanceAccountsList.get(account) + "\"",
                false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        BigInteger balance = new BigInteger(ByteArray
            .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
        System.out.println(token + "_balance_" + account + " : " + balance);
        tokenbalanceList.put(token + "_balance_" + account, balance);
      }
    }
    System.out.println("-------------------------tokenbalance-------------------------");

    System.out.println("---------------------------kLast & getReserves--------------------------");
    Map<String, BigInteger> kLastList = new HashMap<>();
    for (String pair : pairsList.keySet()) {
      transactionExtention = PublicMethed
          .triggerConstantContractForExtention(
              WalletClient.decodeFromBase58Check(pairsList.get(pair)), "kLast()", "", false,
              0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      BigInteger kLast = new BigInteger(ByteArray
          .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
      System.out.println(pair + "_kLast : " + kLast);
      kLastList.put(pair + "_kLast", kLast);
    }

    Map<String, Map<String, BigInteger>> getReservesList = new HashMap<>();
    for (String pair : pairsList.keySet()) {
      transactionExtention = PublicMethed
          .triggerConstantContractForExtention(
              WalletClient.decodeFromBase58Check(pairsList.get(pair)),
              "getReserves()", "", false, 0, 0, "0", 0, testAccountAddress, testAccountKey,
              blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      BigInteger reserve0 = new BigInteger(ByteArray
          .toHexString(transactionExtention.getConstantResult(0).toByteArray())
          .substring(0, 64), 16);
      System.out.println(pair + "_reserve0 : " + reserve0);
      BigInteger reserve1 = new BigInteger(ByteArray
          .toHexString(transactionExtention.getConstantResult(0).toByteArray())
          .substring(65, 128), 16);
      System.out.println(pair + "_reserve1 : " + reserve1);
      BigInteger blockTimestampLast = new BigInteger(ByteArray
          .toHexString(transactionExtention.getConstantResult(0).toByteArray())
          .substring(129, 192), 16);
      System.out.println(pair + "_blockTimestampLast : " + blockTimestampLast);
      Map<String, BigInteger> reserveInfoMap = new HashMap<>();
      reserveInfoMap.put("reserve0", reserve0);
      reserveInfoMap.put("reserve1", reserve1);
      reserveInfoMap.put("blockTimestampLast", blockTimestampLast);
      getReservesList.put(pair + "_ReserveInfo", reserveInfoMap);
    }
    System.out.println("---------------------------kLast & getReserves--------------------------");

    /*// swapExactTokensForTokens
    String[] path = {TUSD, WTRX, SUN, USDJ};
//    BigInteger swapInAmount = new BigInteger("1000000000000");
    BigInteger swapInAmount = new BigInteger("100000000000000000");
    txid = swapExactTokensForTokens(swapInAmount, new BigInteger("1"), path,
        WalletClient.encode58Check(testAccountAddress));
    info = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    BigInteger subAmountTUSDForSwap = new BigInteger(
        ByteArray.toHexString(info.get().getContractResult(0).toByteArray()).substring(128, 192),
        16);
    BigInteger amountWTRXForSwap = new BigInteger(
        ByteArray.toHexString(info.get().getContractResult(0).toByteArray()).substring(192, 256),
        16);
    BigInteger amountSUNForSwap = new BigInteger(
        ByteArray.toHexString(info.get().getContractResult(0).toByteArray()).substring(256, 320),
        16);
    BigInteger addAmountUSDJForSwap = new BigInteger(
        ByteArray.toHexString(info.get().getContractResult(0).toByteArray()).substring(320, 384),
        16);
    Assert.assertEquals(swapInAmount, subAmountTUSDForSwap);
    System.out.println(
        "subAmountTUSDForSwap:" + subAmountTUSDForSwap + "   amountWTRXForSwap:" + amountWTRXForSwap
            + "   amountSUNForSwap:" + amountSUNForSwap + "   addAmountUSDJForSwap:"
            + addAmountUSDJForSwap);

    // kLast---USDJ_SUN-after
    System.out.println("-------------------------kLast & getReserves-------------------------");
    Map<String, BigInteger> kLastMidList = new HashMap<>();
    for (String pair : pairsList.keySet()) {
      transactionExtention = PublicMethed
          .triggerConstantContractForExtention(
              WalletClient.decodeFromBase58Check(pairsList.get(pair)), "kLast()", "", false,
              0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      BigInteger kLast = new BigInteger(ByteArray
          .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
      System.out.println(pair + "_kLastMid : " + kLast);
      kLastMidList.put(pair + "_kLastMid", kLast);
    }
    Map<String, Map<String, BigInteger>> getReservesMidList = new HashMap<>();
    for (String pair : pairsList.keySet()) {
      transactionExtention = PublicMethed
          .triggerConstantContractForExtention(
              WalletClient.decodeFromBase58Check(pairsList.get(pair)),
              "getReserves()", "", false, 0, 0, "0", 0, testAccountAddress, testAccountKey,
              blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      BigInteger reserve0 = new BigInteger(ByteArray
          .toHexString(transactionExtention.getConstantResult(0).toByteArray())
          .substring(0, 64), 16);
      System.out.println(pair + "_reserve0Mid : " + reserve0);
      BigInteger reserve1 = new BigInteger(ByteArray
          .toHexString(transactionExtention.getConstantResult(0).toByteArray())
          .substring(65, 128), 16);
      System.out.println(pair + "_reserve1Mid : " + reserve1);
      BigInteger blockTimestampLast = new BigInteger(ByteArray
          .toHexString(transactionExtention.getConstantResult(0).toByteArray())
          .substring(129, 192), 16);
      System.out.println(pair + "_blockTimestampLastMid : " + blockTimestampLast);
      Map<String, BigInteger> reserveInfoMap = new HashMap<>();
      reserveInfoMap.put("reserve0Mid", reserve0);
      reserveInfoMap.put("reserve1Mid", reserve1);
      reserveInfoMap.put("blockTimestampLastMid", blockTimestampLast);
      getReservesMidList.put(pair + "_ReserveInfoMid", reserveInfoMap);
    }
    System.out.println("-------------------------kLast & getReserves-------------------------");

    // addLiquidity
    txid = addLiquidity(TUSD, WTRX, new BigInteger("1000000000000"), new BigInteger("2000000"),
        new BigInteger("1"), new BigInteger("1"), WalletClient.encode58Check(testAccountAddress));
    info = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    BigInteger removeAmountAForAddTUSD_WTRXLp = new BigInteger(
        ByteArray.toHexString(info.get().getContractResult(0).toByteArray()).substring(0, 64), 16);
    BigInteger removeAmountBForAddTUSD_WTRXLp = new BigInteger(
        ByteArray.toHexString(info.get().getContractResult(0).toByteArray()).substring(64, 128),
        16);
    BigInteger addLpAmountForAddTUSD_WTRXLp = new BigInteger(
        ByteArray.toHexString(info.get().getContractResult(0).toByteArray()).substring(128, 192),
        16);
    System.out.println(
        "removeAmountAForAddTUSD_WTRXLp:" + removeAmountAForAddTUSD_WTRXLp
            + "   removeAmountBForAddTUSD_WTRXLp:" + removeAmountBForAddTUSD_WTRXLp
            + "   addLpAmountForAddTUSD_WTRXLp:" + addLpAmountForAddTUSD_WTRXLp);

    // removeLiquidity
    txid = removeLiquidity(SUN, WTRX, new BigInteger("2000000"), new BigInteger("1"),
        new BigInteger("1"), WalletClient.encode58Check(testAccountAddress));
    info = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    BigInteger addAmountAForRemoveSUN_WTRXLp = new BigInteger(
        ByteArray.toHexString(info.get().getContractResult(0).toByteArray()).substring(0, 64), 16);
    BigInteger addAmountBForRemoveSUN_WTRXLp = new BigInteger(
        ByteArray.toHexString(info.get().getContractResult(0).toByteArray()).substring(64, 128),
        16);
    System.out.println(
        "addAmountAForRemoveSUN_WTRXLp:" + addAmountAForRemoveSUN_WTRXLp
            + "   addAmountBForRemoveSUN_WTRXLp:" + addAmountBForRemoveSUN_WTRXLp);

    // addLiquidity
    txid = addLiquidity(SUN, USDJ, new BigInteger("5000000000000"), new BigInteger("2000000"),
        new BigInteger("1"), new BigInteger("1"), WalletClient.encode58Check(testAccountAddress));
    info = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    BigInteger removeAmountAForAddSUN_USDJLp = new BigInteger(
        ByteArray.toHexString(info.get().getContractResult(0).toByteArray()).substring(0, 64), 16);
    BigInteger removeAmountBForAddSUN_USDJLp = new BigInteger(
        ByteArray.toHexString(info.get().getContractResult(0).toByteArray()).substring(64, 128),
        16);
    BigInteger addLpAmountForAddSUN_USDJLp = new BigInteger(
        ByteArray.toHexString(info.get().getContractResult(0).toByteArray()).substring(128, 192),
        16);
    System.out.println(
        "removeAmountAForAddSUN_USDJLp:" + removeAmountAForAddSUN_USDJLp
            + "   removeAmountBForAddSUN_USDJLp:" + removeAmountBForAddSUN_USDJLp
            + "   addLpAmountForAddSUN_USDJLp:" + addLpAmountForAddSUN_USDJLp);

    // totalSupply---after
    System.out.println("-------------------------lpTotalSupply-------------------------");
    Map<String, BigInteger> totalSupplyAfterList = new HashMap<>();
    for (String pair : pairsList.keySet()) {
      transactionExtention = PublicMethed
          .triggerConstantContractForExtention(
              WalletClient.decodeFromBase58Check(pairsList.get(pair)), "totalSupply()", "", false,
              0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      BigInteger lpTotalSupply = new BigInteger(ByteArray
          .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
      System.out.println(pair + "_lpTotalSupplyAfter : " + lpTotalSupply);
      totalSupplyAfterList.put(pair + "_lpTotalSupplyAfter", lpTotalSupply);
    }
    System.out.println("-------------------------lpTotalSupply-------------------------");

    // lpBalance-account-after
    System.out.println("-------------------------lpBalance-------------------------");
    Map<String, BigInteger> lpbalanceAfterList = new HashMap<>();
    for (String pair : pairsList.keySet()) {
      for (String account : checkLpBalanceAccountsList.keySet()) {
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(
                WalletClient.decodeFromBase58Check(pairsList.get(pair)),
                "balanceOf(address)", "\"" + checkLpBalanceAccountsList.get(account) + "\"",
                false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        BigInteger balance = new BigInteger(ByteArray
            .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
        System.out.println(pair + "_LpbalanceAfter_" + account + " : " + balance);
        lpbalanceAfterList.put(pair + "_LpbalanceAfter_" + account, balance);
      }
    }
    // fee == feeTo.lpbalanceAfter - feeTo.lpbalance
    BigInteger USDJ_SUN_fee = lpbalanceAfterList.get("USDJ_SUN_LpbalanceAfter_feeTo")
        .subtract(lpbalanceList.get("USDJ_SUN_Lpbalance_feeTo"));
    BigInteger SUN_TRX_fee = lpbalanceAfterList.get("SUN_TRX_LpbalanceAfter_feeTo")
        .subtract(lpbalanceList.get("SUN_TRX_Lpbalance_feeTo"));
    BigInteger TUSD_TRX_fee = lpbalanceAfterList.get("TUSD_TRX_LpbalanceAfter_feeTo")
        .subtract(lpbalanceList.get("TUSD_TRX_Lpbalance_feeTo"));
    System.out.println("USDJ_SUN_fee: " + USDJ_SUN_fee);
    System.out.println("SUN_TRX_fee: " + SUN_TRX_fee);
    System.out.println("TUSD_TRX_fee: " + TUSD_TRX_fee);
    // account1.lpbalance - removeLpAmount == account1.lpbalanceAfter
    Assert.assertEquals(
        lpbalanceList.get("SUN_TRX_Lpbalance_account1").subtract(new BigInteger("2000000")),
        lpbalanceAfterList.get("SUN_TRX_LpbalanceAfter_account1"));
    // account1.lpbalance + addLpAmount == account1.lpbalanceAfter
    Assert.assertEquals(
        lpbalanceList.get("USDJ_SUN_Lpbalance_account1").add(addLpAmountForAddSUN_USDJLp),
        lpbalanceAfterList.get("USDJ_SUN_LpbalanceAfter_account1"));
    Assert.assertEquals(
        lpbalanceList.get("TUSD_TRX_Lpbalance_account1").add(addLpAmountForAddTUSD_WTRXLp),
        lpbalanceAfterList.get("TUSD_TRX_LpbalanceAfter_account1"));
    // feeTo.lpbalanceAfter + account1.lpbalanceAfter + account2.lpbalanceAfter == lp.totalSupplyAfter-1000
    Assert.assertEquals(
        lpbalanceAfterList.get("USDJ_SUN_LpbalanceAfter_feeTo")
            .add(lpbalanceAfterList.get("USDJ_SUN_LpbalanceAfter_account1"))
            .add(lpbalanceAfterList.get("USDJ_SUN_LpbalanceAfter_account2")),
        totalSupplyAfterList.get("USDJ_SUN_lpTotalSupplyAfter").subtract(new BigInteger("1000")));
    Assert.assertEquals(
        lpbalanceAfterList.get("SUN_TRX_LpbalanceAfter_feeTo")
            .add(lpbalanceAfterList.get("SUN_TRX_LpbalanceAfter_account1"))
            .add(lpbalanceAfterList.get("SUN_TRX_LpbalanceAfter_account2")),
        totalSupplyAfterList.get("SUN_TRX_lpTotalSupplyAfter").subtract(new BigInteger("1000")));
    Assert.assertEquals(
        lpbalanceAfterList.get("TUSD_TRX_LpbalanceAfter_feeTo")
            .add(lpbalanceAfterList.get("TUSD_TRX_LpbalanceAfter_account1"))
            .add(lpbalanceAfterList.get("TUSD_TRX_LpbalanceAfter_account2")),
        totalSupplyAfterList.get("TUSD_TRX_lpTotalSupplyAfter").subtract(new BigInteger("1000")));
    // lp.totalSupply - removeLpAmount + fee == lp.totalSupplyAfter
    Assert.assertEquals(
        totalSupplyList.get("SUN_TRX_lpTotalSupply").subtract(new BigInteger("2000000"))
            .add(SUN_TRX_fee),
        totalSupplyAfterList.get("SUN_TRX_lpTotalSupplyAfter"));
    // lp.totalSupply + addLpAmount + fee == lp.totalSupplyAfter
    Assert.assertEquals(
        totalSupplyList.get("USDJ_SUN_lpTotalSupply").add(addLpAmountForAddSUN_USDJLp)
            .add(USDJ_SUN_fee),
        totalSupplyAfterList.get("USDJ_SUN_lpTotalSupplyAfter"));
    Assert.assertEquals(
        totalSupplyList.get("TUSD_TRX_lpTotalSupply").add(addLpAmountForAddTUSD_WTRXLp)
            .add(TUSD_TRX_fee),
        totalSupplyAfterList.get("TUSD_TRX_lpTotalSupplyAfter"));
    System.out.println("-------------------------lpBalance-------------------------");

    // tokenbalance-after
    System.out.println("-------------------------tokenbalance-------------------------");
    Map<String, BigInteger> tokenbalanceAfterList = new HashMap<>();
    for (String token : tokensList.keySet()) {
      for (String account : checkTokenBalanceAccountsList.keySet()) {
        transactionExtention = PublicMethed
            .triggerConstantContractForExtention(
                WalletClient.decodeFromBase58Check(tokensList.get(token)),
                "balanceOf(address)", "\"" + checkTokenBalanceAccountsList.get(account) + "\"",
                false, 0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
        Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
        BigInteger balance = new BigInteger(ByteArray
            .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
        System.out.println(token + "_balanceAfter_" + account + " : " + balance);
        tokenbalanceAfterList.put(token + "_balanceAfter_" + account, balance);
      }
    }
    // SUN_TRX.WTRXbalance + amountWTRXForSwap - addAmountBForRemoveSUN_WTRXLp == SUN_TRX.WTRXbalanceAfter
    Assert
        .assertEquals(tokenbalanceList.get("WTRX_balance_SUN_TRX").add(amountWTRXForSwap)
                .subtract(addAmountBForRemoveSUN_WTRXLp),
            tokenbalanceAfterList.get("WTRX_balanceAfter_SUN_TRX"));
    // SUN_TRX.SUNbalance - amountSUNForSwap - addAmountAForRemoveSUN_WTRXLp == SUN_TRX.SUNbalanceAfter
    Assert
        .assertEquals(tokenbalanceList.get("SUN_balance_SUN_TRX").subtract(amountSUNForSwap)
                .subtract(addAmountAForRemoveSUN_WTRXLp),
            tokenbalanceAfterList.get("SUN_balanceAfter_SUN_TRX"));
    // USDJ_SUN.SUNbalance + amountSUNForSwap + removeAmountAForAddSUN_USDJLp == USDJ_SUN.SUNbalanceAfter
    Assert
        .assertEquals(tokenbalanceList.get("SUN_balance_USDJ_SUN").add(amountSUNForSwap)
                .add(removeAmountAForAddSUN_USDJLp),
            tokenbalanceAfterList.get("SUN_balanceAfter_USDJ_SUN"));
    // USDJ_SUN.USDJbalance - addAmountUSDJForSwap + removeAmountBForAddSUN_USDJLp == USDJ_SUN.USDJbalanceAfter
    Assert.assertEquals(tokenbalanceList.get("USDJ_balance_USDJ_SUN").subtract(addAmountUSDJForSwap)
            .add(removeAmountBForAddSUN_USDJLp),
        tokenbalanceAfterList.get("USDJ_balanceAfter_USDJ_SUN"));
    // TUSD_TRX.TUSDbalance + subAmountTUSDForSwap + removeAmountAForAddTUSD_WTRXLp == TUSD_TRX.TUSDbalanceAfter
    Assert
        .assertEquals(tokenbalanceList.get("TUSD_balance_TUSD_TRX").add(subAmountTUSDForSwap)
                .add(removeAmountAForAddTUSD_WTRXLp),
            tokenbalanceAfterList.get("TUSD_balanceAfter_TUSD_TRX"));
    // TUSD_TRX.WTRXbalance - amountWTRXForSwap + removeAmountBForAddTUSD_WTRXLp == TUSD_TRX.WTRXbalanceAfter
    Assert.assertEquals(tokenbalanceList.get("WTRX_balance_TUSD_TRX").subtract(amountWTRXForSwap)
            .add(removeAmountBForAddTUSD_WTRXLp),
        tokenbalanceAfterList.get("WTRX_balanceAfter_TUSD_TRX"));
    // account1.TUSDbalance - subAmountTUSDForSwap - removeAmountAForAddTUSD_WTRXLp == account1.TUSDbalanceAfter
    Assert
        .assertEquals(tokenbalanceList.get("TUSD_balance_account1").subtract(subAmountTUSDForSwap)
                .subtract(removeAmountAForAddTUSD_WTRXLp),
            tokenbalanceAfterList.get("TUSD_balanceAfter_account1"));
    // account1.WTRXbalance - removeAmountBForAddTUSD_WTRXLp + addAmountBForRemoveSUN_WTRXLp == account1.WTRXbalanceAfter
    Assert.assertEquals(
        tokenbalanceList.get("WTRX_balance_account1").subtract(removeAmountBForAddTUSD_WTRXLp)
            .add(addAmountBForRemoveSUN_WTRXLp),
        tokenbalanceAfterList.get("WTRX_balanceAfter_account1"));
    // account1.USDJbalance + addAmountUSDJForSwap - removeAmountBForAddSUN_USDJLp == account1.USDJbalanceAfter
    Assert.assertEquals(tokenbalanceList.get("USDJ_balance_account1").add(addAmountUSDJForSwap)
            .subtract(removeAmountBForAddSUN_USDJLp),
        tokenbalanceAfterList.get("USDJ_balanceAfter_account1"));
    // account1.SUNbalance + addAmountAForRemoveSUN_WTRXLp - removeAmountAForAddSUN_USDJLp == account1.SUNbalanceAfter
    Assert.assertEquals(
        tokenbalanceList.get("SUN_balance_account1").add(addAmountAForRemoveSUN_WTRXLp)
            .subtract(removeAmountAForAddSUN_USDJLp),
        tokenbalanceAfterList.get("SUN_balanceAfter_account1"));
    System.out.println("-------------------------tokenbalance-------------------------");

    // kLast---USDJ_SUN-after
    System.out.println("-------------------------kLast & getReserves-------------------------");
    Map<String, BigInteger> kLastAfterList = new HashMap<>();
    for (String pair : pairsList.keySet()) {
      transactionExtention = PublicMethed
          .triggerConstantContractForExtention(
              WalletClient.decodeFromBase58Check(pairsList.get(pair)), "kLast()", "", false,
              0, 0, "0", 0, testAccountAddress, testAccountKey, blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      BigInteger kLast = new BigInteger(ByteArray
          .toHexString(transactionExtention.getConstantResult(0).toByteArray()), 16);
      System.out.println(pair + "_kLastAfter : " + kLast);
      kLastAfterList.put(pair + "_kLastAfter", kLast);
    }
    Map<String, Map<String, BigInteger>> getReservesAfterList = new HashMap<>();
    for (String pair : pairsList.keySet()) {
      transactionExtention = PublicMethed
          .triggerConstantContractForExtention(
              WalletClient.decodeFromBase58Check(pairsList.get(pair)),
              "getReserves()", "", false, 0, 0, "0", 0, testAccountAddress, testAccountKey,
              blockingStubFull);
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      BigInteger reserve0 = new BigInteger(ByteArray
          .toHexString(transactionExtention.getConstantResult(0).toByteArray())
          .substring(0, 64), 16);
      System.out.println(pair + "_reserve0After : " + reserve0);
      BigInteger reserve1 = new BigInteger(ByteArray
          .toHexString(transactionExtention.getConstantResult(0).toByteArray())
          .substring(65, 128), 16);
      System.out.println(pair + "_reserve1After : " + reserve1);
      BigInteger blockTimestampLast = new BigInteger(ByteArray
          .toHexString(transactionExtention.getConstantResult(0).toByteArray())
          .substring(129, 192), 16);
      System.out.println(pair + "_blockTimestampLastAfter : " + blockTimestampLast);
      Map<String, BigInteger> reserveInfoMap = new HashMap<>();
      reserveInfoMap.put("reserve0After", reserve0);
      reserveInfoMap.put("reserve1After", reserve1);
      reserveInfoMap.put("blockTimestampLastAfter", blockTimestampLast);
      getReservesAfterList.put(pair + "_ReserveInfoAfter", reserveInfoMap);
    }
    // USDJ_SUN.SUN_balanceAfter == pair.reserve0After
    Assert.assertEquals(tokenbalanceAfterList.get("SUN_balanceAfter_USDJ_SUN"),
        getReservesAfterList.get("USDJ_SUN_ReserveInfoAfter").get("reserve0After"));
    // USDJ_SUN.USDJ_balanceAfter == pair.reserve1After
    Assert.assertEquals(tokenbalanceAfterList.get("USDJ_balanceAfter_USDJ_SUN"),
        getReservesAfterList.get("USDJ_SUN_ReserveInfoAfter").get("reserve1After"));
    // SUN_TRX.SUN_balanceAfter == pair.reserve0After
    Assert.assertEquals(tokenbalanceAfterList.get("SUN_balanceAfter_SUN_TRX"),
        getReservesAfterList.get("SUN_TRX_ReserveInfoAfter").get("reserve0After"));
    // SUN_TRX.WTRX_balanceAfter == pair.reserve1After
    Assert.assertEquals(tokenbalanceAfterList.get("WTRX_balanceAfter_SUN_TRX"),
        getReservesAfterList.get("SUN_TRX_ReserveInfoAfter").get("reserve1After"));
    // TUSD_TRX.TUSD_balanceAfter == pair.reserve0After
    Assert.assertEquals(tokenbalanceAfterList.get("TUSD_balanceAfter_TUSD_TRX"),
        getReservesAfterList.get("TUSD_TRX_ReserveInfoAfter").get("reserve0After"));
    // TUSD_TRX.WTRX_balanceAfter == pair.reserve1After
    Assert.assertEquals(tokenbalanceAfterList.get("WTRX_balanceAfter_TUSD_TRX"),
        getReservesAfterList.get("TUSD_TRX_ReserveInfoAfter").get("reserve1After"));
    System.out.println("-------------------------kLast & getReserves-------------------------");*/
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


  public String addLiquidity(String tokenA, String tokenB, BigInteger amountADesired,
      BigInteger amountBDesired, BigInteger amountAMin, BigInteger amountBMin, String to) {
    String txid;
    String param;
    Optional<TransactionInfo> infoById;
    param =
        "\"" + tokenA + "\",\"" + tokenB + "\"," + amountADesired + "," + amountBDesired + ","
            + amountAMin + "," + amountBMin + ",\"" + to + "\"," + getTimestamp("2021-12-31");
    txid = PublicMethed
        .triggerContract(WalletClient.decodeFromBase58Check(UniswapV2Router02),
            "addLiquidity(address,address,uint256,uint256,uint256,uint256,address,uint256)",
            param, false, 0, maxFeeLimit, testAccountAddress, testAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
    return txid;
  }

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
