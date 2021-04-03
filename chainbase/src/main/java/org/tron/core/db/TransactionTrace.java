package org.tron.core.db;

import static org.tron.common.runtime.InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE;
import static org.tron.common.runtime.InternalTransaction.TrxType.TRX_CONTRACT_CREATION_TYPE;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.spongycastle.util.encoders.Hex;
import org.springframework.util.StringUtils;
import org.tron.common.runtime.InternalTransaction.TrxType;
import org.tron.common.runtime.ProgramResult;
import org.tron.common.runtime.Runtime;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.utils.Commons;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.FastByteComparisons;
import org.tron.common.utils.ForkController;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.WalletUtil;
import org.tron.core.Constant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.DelegatedResourceAccountIndexCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.capsule.ReceiptCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.Parameter;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.store.AccountAssetIssueStore;
import org.tron.core.store.AccountStore;
import org.tron.core.store.CodeStore;
import org.tron.core.store.ContractStore;
import org.tron.core.store.DelegatedResourceAccountIndexStore;
import org.tron.core.store.DelegatedResourceStore;
import org.tron.core.store.DelegationStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.StoreFactory;
import org.tron.core.store.VotesStore;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;

@Slf4j(topic = "TransactionTrace")
public class TransactionTrace {

  private TransactionCapsule trx;

  private ReceiptCapsule receipt;

  private StoreFactory storeFactory;

  private DynamicPropertiesStore dynamicPropertiesStore;

  private ContractStore contractStore;

  private AccountStore accountStore;

  private DelegatedResourceAccountIndexStore delegatedResourceAccountIndexStore;

  private DelegatedResourceStore delegatedResourceStore;

  private AccountAssetIssueStore accountAssetIssueStore;

  private CodeStore codeStore;

  private EnergyProcessor energyProcessor;

  private TrxType trxType;

  private Runtime runtime;

  private ForkController forkController;

  private VotesStore votesStore;

  private DelegationStore delegationStore;

  @Getter
  private TransactionContext transactionContext;
  @Getter
  @Setter
  private TimeResultType timeResultType = TimeResultType.NORMAL;
  @Getter
  @Setter
  private boolean netFeeForBandwidth = true;

  public TransactionTrace(TransactionCapsule trx, StoreFactory storeFactory,
      Runtime runtime) {
    this.trx = trx;
    Transaction.Contract.ContractType contractType = this.trx.getInstance().getRawData()
        .getContract(0).getType();
    switch (contractType.getNumber()) {
      case ContractType.TriggerSmartContract_VALUE:
        trxType = TRX_CONTRACT_CALL_TYPE;
        break;
      case ContractType.CreateSmartContract_VALUE:
        trxType = TRX_CONTRACT_CREATION_TYPE;
        break;
      default:
        trxType = TrxType.TRX_PRECOMPILED_TYPE;
    }
    this.storeFactory = storeFactory;
    this.dynamicPropertiesStore = storeFactory.getChainBaseManager().getDynamicPropertiesStore();
    this.contractStore = storeFactory.getChainBaseManager().getContractStore();
    this.codeStore = storeFactory.getChainBaseManager().getCodeStore();
    this.accountStore = storeFactory.getChainBaseManager().getAccountStore();
    this.delegatedResourceAccountIndexStore = storeFactory.getChainBaseManager()
        .getDelegatedResourceAccountIndexStore();
    this.delegatedResourceStore = storeFactory.getChainBaseManager().getDelegatedResourceStore();
    this.accountAssetIssueStore = storeFactory.getChainBaseManager().getAccountAssetIssueStore();

    this.receipt = new ReceiptCapsule(Sha256Hash.ZERO_HASH);
    this.energyProcessor = new EnergyProcessor(dynamicPropertiesStore, accountStore);
    this.runtime = runtime;
    this.forkController = new ForkController();
    forkController.init(storeFactory.getChainBaseManager());

    this.votesStore = storeFactory.getChainBaseManager().getVotesStore();
    this.delegationStore = storeFactory.getChainBaseManager().getDelegationStore();
  }

  public TransactionCapsule getTrx() {
    return trx;
  }

  private boolean needVM() {
    return this.trxType == TRX_CONTRACT_CALL_TYPE
        || this.trxType == TRX_CONTRACT_CREATION_TYPE;
  }

  public void init(BlockCapsule blockCap) {
    init(blockCap, false);
  }

  //pre transaction check
  public void init(BlockCapsule blockCap, boolean eventPluginLoaded) {
    transactionContext = new TransactionContext(blockCap, trx, storeFactory, false,
        eventPluginLoaded);
  }

  public void checkIsConstant() throws ContractValidateException, VMIllegalException {
    if (dynamicPropertiesStore.getAllowTvmConstantinople() == 1) {
      return;
    }
    TriggerSmartContract triggerContractFromTransaction = ContractCapsule
        .getTriggerContractFromTransaction(this.getTrx().getInstance());
    if (TRX_CONTRACT_CALL_TYPE == this.trxType) {
      ContractCapsule contract = contractStore
          .get(triggerContractFromTransaction.getContractAddress().toByteArray());
      if (contract == null) {
        logger.info("contract: {} is not in contract store", StringUtil
            .encode58Check(triggerContractFromTransaction.getContractAddress().toByteArray()));
        throw new ContractValidateException("contract: " + StringUtil
            .encode58Check(triggerContractFromTransaction.getContractAddress().toByteArray())
            + " is not in contract store");
      }
      ABI abi = contract.getInstance().getAbi();
      if (WalletUtil.isConstant(abi, triggerContractFromTransaction)) {
        throw new VMIllegalException("cannot call constant method");
      }
    }
  }

  //set bill
  public void setBill(long energyUsage) {
    if (energyUsage < 0) {
      energyUsage = 0L;
    }
    receipt.setEnergyUsageTotal(energyUsage);
  }

  //set net bill
  public void setNetBill(long netUsage, long netFee) {
    receipt.setNetUsage(netUsage);
    receipt.setNetFee(netFee);
  }

  public void setNetBillForCreateNewAccount(long netUsage, long netFee) {
    receipt.setNetUsage(netUsage);
    receipt.setNetFee(netFee);
    setNetFeeForBandwidth(false);
  }

  public void addNetBill(long netFee) {
    receipt.addNetFee(netFee);
  }

  public void exec()
      throws ContractExeException, ContractValidateException, VMIllegalException {
    /*  VM execute  */
    if (dynamicPropertiesStore.getAllowTvmFreeze() == 1) {
      byte[] originAccount;
      byte[] callerAccount;
      switch (trxType) {
        case TRX_CONTRACT_CREATION_TYPE:
          callerAccount = TransactionCapsule
              .getOwner(trx.getInstance().getRawData().getContract(0));
          originAccount = callerAccount;
          receipt.setOriginEnergyLeft(
              energyProcessor.getAccountLeftEnergyFromFreeze(accountStore.get(originAccount)));
          receipt.setCallerEnergyLeft(
              energyProcessor.getAccountLeftEnergyFromFreeze(accountStore.get(callerAccount)));
          break;
        case TRX_CONTRACT_CALL_TYPE:
          TriggerSmartContract callContract = ContractCapsule
              .getTriggerContractFromTransaction(trx.getInstance());
          ContractCapsule contractCapsule =
              contractStore.get(callContract.getContractAddress().toByteArray());
          callerAccount = callContract.getOwnerAddress().toByteArray();
          originAccount = contractCapsule.getOriginAddress();
          receipt.setOriginEnergyLeft(
              energyProcessor.getAccountLeftEnergyFromFreeze(accountStore.get(originAccount)));
          receipt.setCallerEnergyLeft(
              energyProcessor.getAccountLeftEnergyFromFreeze(accountStore.get(callerAccount)));
          break;
        default:
      }
    }
    runtime.execute(transactionContext);
    setBill(transactionContext.getProgramResult().getEnergyUsed());

//    if (TrxType.TRX_PRECOMPILED_TYPE != trxType) {
//      if (contractResult.OUT_OF_TIME
//          .equals(receipt.getResult())) {
//        setTimeResultType(TimeResultType.OUT_OF_TIME);
//      } else if (System.currentTimeMillis() - txStartTimeInMs
//          > CommonParameter.getInstance()
//          .getLongRunningTime()) {
//        setTimeResultType(TimeResultType.LONG_RUNNING);
//      }
//    }
  }

  public void finalization() throws ContractExeException {
    try {
      pay();
      deletedAccountAndResource();
    } catch (BalanceInsufficientException e) {
      throw new ContractExeException(e.getMessage());
    }
  }

  private void deletedAccountAndResource() throws BalanceInsufficientException {
    if (StringUtils.isEmpty(transactionContext.getProgramResult().getRuntimeError())) {
      for (DataWord contract : transactionContext.getProgramResult().getDeleteAccounts()) {
        deleteContract(convertToTronAddress((contract.getLast20Bytes())));
      }
      for (DataWord address : transactionContext.getProgramResult().getDeleteVotes()) {
        votesStore.delete(convertToTronAddress((address.getLast20Bytes())));
      }
      if (dynamicPropertiesStore.getAllowTvmFreeze() == 1) {
        for (Pair<DataWord, DataWord> addressPair : transactionContext.getProgramResult()
            .getDeleteDelegation()) {
          byte[] contract = addressPair.getLeft().getLast20Bytes();
          byte[] obtainer = addressPair.getRight().getLast20Bytes();
          byte[] blackHoleAddress = new DataWord(accountStore.getBlackholeAddress())
              .getLast20Bytes();
          if (FastByteComparisons.isEqual(contract, obtainer)
              || FastByteComparisons.isEqual(obtainer, blackHoleAddress)) {
            transferDelegatedResourceToBlackHole(contract, blackHoleAddress);
          } else {
            transferDelegatedResourceToInheritor(contract, obtainer);
          }
          deleteDelegationByAddress(convertToTronAddress(contract));
        }
      }
    }
  }

  /**
   * pay actually bill(include ENERGY and storage).
   */
  public void pay() throws BalanceInsufficientException {
    byte[] originAccount;
    byte[] callerAccount;
    long percent = 0;
    long originEnergyLimit = 0;
    switch (trxType) {
      case TRX_CONTRACT_CREATION_TYPE:
        callerAccount = TransactionCapsule
            .getOwner(trx.getInstance().getRawData().getContract(0));
        originAccount = callerAccount;
        break;
      case TRX_CONTRACT_CALL_TYPE:
        TriggerSmartContract callContract = ContractCapsule
            .getTriggerContractFromTransaction(trx.getInstance());
        ContractCapsule contractCapsule =
            contractStore.get(callContract.getContractAddress().toByteArray());

        callerAccount = callContract.getOwnerAddress().toByteArray();
        originAccount = contractCapsule.getOriginAddress();
        percent = Math
            .max(Constant.ONE_HUNDRED - contractCapsule.getConsumeUserResourcePercent(), 0);
        percent = Math.min(percent, Constant.ONE_HUNDRED);
        originEnergyLimit = contractCapsule.getOriginEnergyLimit();
        break;
      default:
        return;
    }

    // originAccount Percent = 30%
    AccountCapsule origin = accountStore.get(originAccount);
    AccountCapsule caller = accountStore.get(callerAccount);
    receipt.payEnergyBill(
        dynamicPropertiesStore, accountStore, forkController,
        origin,
        caller,
        percent, originEnergyLimit,
        energyProcessor,
        EnergyProcessor.getHeadSlot(dynamicPropertiesStore));
  }

  public boolean checkNeedRetry() {
    if (!needVM()) {
      return false;
    }
    return trx.getContractRet() != contractResult.OUT_OF_TIME && receipt.getResult()
        == contractResult.OUT_OF_TIME;
  }

  public void check() throws ReceiptCheckErrException {
    if (!needVM()) {
      return;
    }
    if (Objects.isNull(trx.getContractRet())) {
      throw new ReceiptCheckErrException("null resultCode");
    }
    if (!trx.getContractRet().equals(receipt.getResult())) {
      logger.info(
          "this tx id: {}, the resultCode in received block: {}, the resultCode in self: {}",
          Hex.toHexString(trx.getTransactionId().getBytes()), trx.getContractRet(),
          receipt.getResult());
      throw new ReceiptCheckErrException("Different resultCode");
    }
  }

  public ReceiptCapsule getReceipt() {
    return receipt;
  }

  public void setResult() {
    if (!needVM()) {
      return;
    }
    receipt.setResult(transactionContext.getProgramResult().getResultCode());
  }

  public String getRuntimeError() {
    return transactionContext.getProgramResult().getRuntimeError();
  }

  public ProgramResult getRuntimeResult() {
    return transactionContext.getProgramResult();
  }

  public Runtime getRuntime() {
    return runtime;
  }

  public void deleteContract(byte[] address) {
    codeStore.delete(address);
    accountStore.delete(address);
    contractStore.delete(address);
    accountAssetIssueStore.delete(address);
  }

  public static byte[] convertToTronAddress(byte[] address) {
    if (address.length == 20) {
      byte[] newAddress = new byte[21];
      byte[] temp = new byte[]{DecodeUtil.addressPreFixByte};
      System.arraycopy(temp, 0, newAddress, 0, temp.length);
      System.arraycopy(address, 0, newAddress, temp.length, address.length);
      address = newAddress;
    }
    return address;
  }

  public void deleteDelegationByAddress(byte[] address) {
    delegationStore.delete(address); //begin Cycle
    delegationStore
        .delete(("lastWithdraw-" + Hex.toHexString(address)).getBytes()); //last Withdraw cycle
    delegationStore.delete(("end-" + Hex.toHexString(address)).getBytes()); //end cycle
  }

  public enum TimeResultType {
    NORMAL,
    LONG_RUNNING,
    OUT_OF_TIME
  }

  private void transferDelegatedResourceToBlackHole(byte[] ownerAddr, byte[] blackHoleAddr)
      throws BalanceInsufficientException {

    // delegated resource from sender to owner, just abandon
    // in order to making that sender can unfreeze their balance in future
    // nothing will be deleted

    // process delegated resource from owner to receiver
    long totalDelegatedFrozenBalance = 0;
    DelegatedResourceAccountIndexCapsule indexCapsule = delegatedResourceAccountIndexStore
        .get(ownerAddr);
    for (ByteString receiver : indexCapsule.getToAccountsList()) {
      byte[] receiverAddr = receiver.toByteArray();
      byte[] key = DelegatedResourceCapsule.createDbKey(ownerAddr, receiverAddr);
      DelegatedResourceCapsule delegatedResourceCapsule = delegatedResourceStore.get(key);

      // take back delegated resource from receiver account
      // no need to check like UnfreezeBalanceProcessor
      // because allowTvmFreeze is after allowTvmSolidity059
      AccountCapsule receiverCapsule = accountStore.get(receiverAddr);
      // take back receiver`s delegated bandwidth
      long frozenBalanceForBandwidth = delegatedResourceCapsule.getFrozenBalanceForBandwidth();
      totalDelegatedFrozenBalance += frozenBalanceForBandwidth;
      receiverCapsule
          .safeAddAcquiredDelegatedFrozenBalanceForBandwidth(-frozenBalanceForBandwidth);
      // reduce total net weight
      dynamicPropertiesStore.addTotalNetWeight(-frozenBalanceForBandwidth / TRX_PRECISION);
      // take back receiver`s delegated energy
      long frozenBalanceForEnergy = delegatedResourceCapsule.getFrozenBalanceForEnergy();
      totalDelegatedFrozenBalance += frozenBalanceForEnergy;
      receiverCapsule.safeAddAcquiredDelegatedFrozenBalanceForEnergy(-frozenBalanceForEnergy);
      // reduce total energy weight
      dynamicPropertiesStore.addTotalEnergyWeight(-frozenBalanceForEnergy / TRX_PRECISION);
      accountStore.put(receiverCapsule.createDbKey(), receiverCapsule);

      // remove delegated resource index in receiver`s fromList
      removeOrInsertDelegatedIndexAndUpdate(receiverAddr, ownerAddr, null, ListType.FROM_LIST);

      // set delegated resource to zero
      delegatedResourceCapsule.setFrozenBalanceForBandwidth(0, 0);
      delegatedResourceCapsule.setFrozenBalanceForEnergy(0, 0);
      delegatedResourceStore.put(key, delegatedResourceCapsule);
    }

    // clear owner`s toList
    indexCapsule.setAllToAccounts(new ArrayList<>());
    delegatedResourceAccountIndexStore.put(ownerAddr, indexCapsule);

    // transfer owner`s frozen balance for bandwidth to black hole
    AccountCapsule ownerCapsule = accountStore.get(ownerAddr);
    long frozenBalanceForBandwidthOfOwner = 0;
    // check if frozen for bandwidth exists
    if (ownerCapsule.getFrozenCount() != 0) {
      frozenBalanceForBandwidthOfOwner = ownerCapsule.getFrozenList().get(0).getFrozenBalance();
    }
    dynamicPropertiesStore.addTotalNetWeight(-frozenBalanceForBandwidthOfOwner / TRX_PRECISION);
    long frozenBalanceForEnergyOfOwner =
        ownerCapsule.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance();
    dynamicPropertiesStore.addTotalEnergyWeight(-frozenBalanceForEnergyOfOwner / TRX_PRECISION);

    // TODO: 2021/3/19 是否需要，本来owner账户就要删除了
    // reset owner account
    clearAccountCapsule(ownerCapsule);
    accountStore.put(ownerAddr, ownerCapsule);

    // transfer all kinds of frozen balance to BlackHole
    // TODO add balance to blackHole
    Commons.adjustBalance(accountStore, accountStore.getBlackhole(),
        totalDelegatedFrozenBalance
            + frozenBalanceForBandwidthOfOwner
            + frozenBalanceForEnergyOfOwner);
//    repo.addBalance(blackHoleAddr, totalDelegatedFrozenBalance
//        + frozenBalanceForBandwidthOfOwner
//        + frozenBalanceForEnergyOfOwner);
  }

  private void transferDelegatedResourceToInheritor(byte[] ownerAddr, byte[] inheritorAddr) {
    AccountCapsule inheritorCapsule = accountStore.get(inheritorAddr);
    DelegatedResourceAccountIndexCapsule indexCapsule = delegatedResourceAccountIndexStore
        .get(ownerAddr);

    // process delegated resource from sender to owner
    for (ByteString sender : indexCapsule.getFromAccountsList()) {
      byte[] senderAddr = sender.toByteArray();

      // if sender == inheritor, just abandon this part of resource and do nothing
      if (Arrays.equals(senderAddr, inheritorAddr)) {
        continue;
      }

      byte[] senderToOwnerKey = DelegatedResourceCapsule.createDbKey(senderAddr, ownerAddr);
      DelegatedResourceCapsule senderToOwnerRes = delegatedResourceStore.get(senderToOwnerKey);

      byte[] senderToInheritorKey = DelegatedResourceCapsule
          .createDbKey(senderAddr, inheritorAddr);
      DelegatedResourceCapsule senderToInheritorRes = delegatedResourceStore
          .get(senderToInheritorKey);

      /* process delegated resource from sender to inheritor */
      // create a new sender->inheritor delegated resource
      if (senderToInheritorRes == null) {
        senderToInheritorRes = new DelegatedResourceCapsule(
            ByteString.copyFrom(senderAddr),
            ByteString.copyFrom(inheritorAddr));
      }

      // update sender->inheritor delegated resource
      if (senderToOwnerRes.getFrozenBalanceForBandwidth() != 0) { // for non-zero bandwidth
        senderToInheritorRes.addFrozenBalanceForBandwidth(
            senderToOwnerRes.getFrozenBalanceForBandwidth(),
            calculateNewExpireTime(
                senderToInheritorRes.getFrozenBalanceForBandwidth(),
                senderToInheritorRes.getExpireTimeForBandwidth(),
                senderToOwnerRes.getFrozenBalanceForBandwidth(),
                senderToOwnerRes.getExpireTimeForBandwidth())
        );
      }
      if (senderToOwnerRes.getFrozenBalanceForEnergy() != 0) { // for non-zero energy
        senderToInheritorRes.addFrozenBalanceForEnergy(
            senderToOwnerRes.getFrozenBalanceForEnergy(),
            calculateNewExpireTime(
                senderToInheritorRes.getFrozenBalanceForEnergy(),
                senderToInheritorRes.getExpireTimeForEnergy(),
                senderToOwnerRes.getFrozenBalanceForEnergy(),
                senderToOwnerRes.getExpireTimeForEnergy())
        );
      }
      delegatedResourceStore.put(senderToInheritorKey, senderToInheritorRes);

      /* process inheritor account */
      // increase acquired delegated balance for bandwidth or energy
      inheritorCapsule.addAcquiredDelegatedFrozenBalanceForBandwidth(
          senderToOwnerRes.getFrozenBalanceForBandwidth());
      inheritorCapsule.addAcquiredDelegatedFrozenBalanceForEnergy(
          senderToOwnerRes.getFrozenBalanceForEnergy());

      // update delegated resource index for sender`s toList
      removeOrInsertDelegatedIndexAndUpdate(senderAddr, ownerAddr, inheritorAddr,
          ListType.TO_LIST);

      // update delegated resource index for inheritor`s fromList
      removeOrInsertDelegatedIndexAndUpdate(inheritorAddr, null, senderAddr,
          ListType.FROM_LIST);

      // set sender->owner delegated resource to zero
      senderToOwnerRes.setFrozenBalanceForBandwidth(0, 0);
      senderToOwnerRes.setFrozenBalanceForEnergy(0, 0);
      delegatedResourceStore.put(senderToOwnerKey, senderToOwnerRes);
    }

    // process delegated resource from owner to receiver
    for (ByteString receiver : indexCapsule.getToAccountsList()) {
      byte[] receiverAddr = receiver.toByteArray();
      byte[] ownerToReceiverKey = DelegatedResourceCapsule.createDbKey(ownerAddr, receiverAddr);
      DelegatedResourceCapsule ownerToReceiverRes = delegatedResourceStore
          .get(ownerToReceiverKey);

      // if inheritor == receiver, just take this part of resource as resource that inheritor freeze for self
      if (Arrays.equals(inheritorAddr, receiverAddr)) {

        /* process inheritor account */
        // transfer owner`s delegated frozen balance for bandwidth to inheritor`s frozen balance
        long frozenBalanceForBandwidth = 0;
        long expireTimeForBandwidth = 0;
        // check if frozen for bandwidth exists
        if (inheritorCapsule.getFrozenCount() != 0) {
          frozenBalanceForBandwidth = inheritorCapsule.getFrozenList().get(0)
              .getFrozenBalance();
          expireTimeForBandwidth = inheritorCapsule.getFrozenList().get(0).getExpireTime();
        }
        if (ownerToReceiverRes.getFrozenBalanceForBandwidth() != 0) { // for non-zero bandwidth
          inheritorCapsule.setFrozenForBandwidth(
              Math.addExact(frozenBalanceForBandwidth,
                  ownerToReceiverRes.getFrozenBalanceForBandwidth()),
              calculateNewExpireTime(
                  frozenBalanceForBandwidth,
                  expireTimeForBandwidth,
                  ownerToReceiverRes.getFrozenBalanceForBandwidth(),
                  ownerToReceiverRes.getExpireTimeForBandwidth())
          );
        }

        // transfer owner`s delegated frozen balance for energy to inheritor`s frozen balance
        long frozenBalanceForEnergy =
            inheritorCapsule.getAccountResource().getFrozenBalanceForEnergy()
                .getFrozenBalance();
        long expireTimeForEnergy =
            inheritorCapsule.getAccountResource().getFrozenBalanceForEnergy().getExpireTime();
        if (ownerToReceiverRes.getFrozenBalanceForEnergy() != 0) { // for non-zero energy
          inheritorCapsule.setFrozenForEnergy(
              Math.addExact(frozenBalanceForEnergy,
                  ownerToReceiverRes.getFrozenBalanceForEnergy()),
              calculateNewExpireTime(
                  frozenBalanceForEnergy,
                  expireTimeForEnergy,
                  ownerToReceiverRes.getFrozenBalanceForEnergy(),
                  ownerToReceiverRes.getExpireTimeForEnergy())
          );
        }

        // take back inheritor`s delegated bandwidth
        inheritorCapsule.safeAddAcquiredDelegatedFrozenBalanceForBandwidth(
            ownerToReceiverRes.getFrozenBalanceForBandwidth());

        // take back inheritor`s delegated energy
        inheritorCapsule.safeAddAcquiredDelegatedFrozenBalanceForEnergy(
            ownerToReceiverRes.getFrozenBalanceForEnergy());

        /* process delegated resource account index */
        // remove delegated resource index for owner`s toList
        removeOrInsertDelegatedIndexAndUpdate(ownerAddr, receiverAddr, null, ListType.TO_LIST);

        // remove delegated resource index for receiver`s fromList
        removeOrInsertDelegatedIndexAndUpdate(receiverAddr, ownerAddr, null,
            ListType.FROM_LIST);
      } else {
        byte[] inheritorToReceiverKey = DelegatedResourceCapsule
            .createDbKey(inheritorAddr, receiverAddr);
        DelegatedResourceCapsule inheritorToReceiverRes = delegatedResourceStore
            .get(inheritorToReceiverKey);

        /* process delegated resource from inheritor to receiver */
        // create a new inheritor->receiver delegated resource
        if (inheritorToReceiverRes == null) {
          inheritorToReceiverRes = new DelegatedResourceCapsule(
              ByteString.copyFrom(inheritorAddr),
              ByteString.copyFrom(receiverAddr));
        }

        // update inheritor->receiver delegated resource
        if (ownerToReceiverRes.getFrozenBalanceForBandwidth() != 0) { // for non-zero bandwidth
          inheritorToReceiverRes.addFrozenBalanceForBandwidth(
              ownerToReceiverRes.getFrozenBalanceForBandwidth(),
              calculateNewExpireTime(
                  inheritorToReceiverRes.getFrozenBalanceForBandwidth(),
                  inheritorToReceiverRes.getExpireTimeForBandwidth(),
                  ownerToReceiverRes.getFrozenBalanceForBandwidth(),
                  ownerToReceiverRes.getExpireTimeForBandwidth())
          );
        }
        if (ownerToReceiverRes.getFrozenBalanceForEnergy() != 0) { // for non-zero energy
          inheritorToReceiverRes.addFrozenBalanceForEnergy(
              ownerToReceiverRes.getFrozenBalanceForEnergy(),
              calculateNewExpireTime(
                  inheritorToReceiverRes.getFrozenBalanceForEnergy(),
                  inheritorToReceiverRes.getExpireTimeForEnergy(),
                  ownerToReceiverRes.getFrozenBalanceForEnergy(),
                  ownerToReceiverRes.getExpireTimeForEnergy())
          );
        }
        delegatedResourceStore.put(inheritorToReceiverKey, inheritorToReceiverRes);

        /* process inheritor account */
        // increase delegated balance for bandwidth or energy
        inheritorCapsule.addDelegatedFrozenBalanceForBandwidth(
            ownerToReceiverRes.getFrozenBalanceForBandwidth());
        inheritorCapsule.addDelegatedFrozenBalanceForEnergy(
            ownerToReceiverRes.getFrozenBalanceForEnergy());

        /* process delegated resource account index */
        // update delegated resource index for receiver`s fromList
        removeOrInsertDelegatedIndexAndUpdate(receiverAddr, ownerAddr, inheritorAddr,
            ListType.FROM_LIST);

        // update delegated resource index for inheritor`s toList
        removeOrInsertDelegatedIndexAndUpdate(inheritorAddr, null, receiverAddr,
            ListType.TO_LIST);
      }

      // set owner->receiver delegated resource to zero
      ownerToReceiverRes.setFrozenBalanceForBandwidth(0, 0);
      ownerToReceiverRes.setFrozenBalanceForEnergy(0, 0);
      delegatedResourceStore.put(ownerToReceiverKey, ownerToReceiverRes);
    }

    // clear delegated resource index for owner
    indexCapsule.setAllFromAccounts(new ArrayList<>());
    indexCapsule.setAllToAccounts(new ArrayList<>());
    delegatedResourceAccountIndexStore.put(ownerAddr, indexCapsule);

    /* process owner`s frozen balance */
    // transfer owner`s frozen balance for bandwidth to inheritor
    AccountCapsule ownerCapsule = accountStore.get(ownerAddr);
    long frozenBalanceForBandwidthOfOwner = 0;
    long expireTimeForBandwidthOfOwner = 0;
    // check if frozen for bandwidth exists
    if (ownerCapsule.getFrozenCount() != 0) {
      frozenBalanceForBandwidthOfOwner = ownerCapsule.getFrozenList().get(0).getFrozenBalance();
      expireTimeForBandwidthOfOwner = ownerCapsule.getFrozenList().get(0).getExpireTime();
    }
    long frozenBalanceForBandwidthOfInheritor = 0;
    long expireTimeForBandwidthOfInheritor = 0;
    // check if frozen for bandwidth exists
    if (inheritorCapsule.getFrozenCount() != 0) {
      frozenBalanceForBandwidthOfInheritor = inheritorCapsule.getFrozenList().get(0)
          .getFrozenBalance();
      expireTimeForBandwidthOfInheritor = inheritorCapsule.getFrozenList().get(0)
          .getExpireTime();
    }
    if (frozenBalanceForBandwidthOfOwner != 0) { // for non-zero bandwidth
      inheritorCapsule.setFrozenForBandwidth(
          Math.addExact(frozenBalanceForBandwidthOfInheritor,
              frozenBalanceForBandwidthOfOwner),
          calculateNewExpireTime(
              frozenBalanceForBandwidthOfInheritor,
              expireTimeForBandwidthOfInheritor,
              frozenBalanceForBandwidthOfOwner,
              expireTimeForBandwidthOfOwner)
      );
    }

    // transfer owner`s frozen balance for energy to inheritor
    long frozenBalanceForEnergyOfOwner =
        ownerCapsule.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance();
    long expireTimeForEnergyOfOwner =
        ownerCapsule.getAccountResource().getFrozenBalanceForEnergy().getExpireTime();
    long frozenBalanceForEnergyOfInheritor =
        inheritorCapsule.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance();
    long expireTimeForEnergyOfInheritor =
        inheritorCapsule.getAccountResource().getFrozenBalanceForEnergy().getExpireTime();
    if (frozenBalanceForEnergyOfOwner != 0) { // for non-zero energy
      inheritorCapsule.setFrozenForEnergy(
          Math.addExact(frozenBalanceForEnergyOfInheritor,
              frozenBalanceForEnergyOfOwner),
          calculateNewExpireTime(
              frozenBalanceForEnergyOfInheritor,
              expireTimeForEnergyOfInheritor,
              frozenBalanceForEnergyOfOwner,
              expireTimeForEnergyOfOwner)
      );
    }

    // reset owner account and update
    clearAccountCapsule(ownerCapsule);
    accountStore.put(ownerAddr, ownerCapsule);

    // update inheritor account
    accountStore.put(inheritorAddr, inheritorCapsule);
  }

  private void clearAccountCapsule(AccountCapsule accountCapsule) {
    accountCapsule.setAcquiredDelegatedFrozenBalanceForBandwidth(0);
    accountCapsule.setAcquiredDelegatedFrozenBalanceForEnergy(0);
    accountCapsule.setDelegatedFrozenBalanceForBandwidth(0);
    accountCapsule.setDelegatedFrozenBalanceForEnergy(0);
    accountCapsule.setFrozenForBandwidth(0, 0);
    accountCapsule.setFrozenForEnergy(0, 0);
  }

  private long calculateNewExpireTime(
      long originFrozenBalance,
      long originExpireTime,
      long addedFrozenBalance,
      long addedExpireTime) {
    long now = transactionContext.getBlockCap().getTimeStamp();
    long maxExpire =
        dynamicPropertiesStore.getMinFrozenTime() * Parameter.ChainConstant.FROZEN_PERIOD;

    if (originFrozenBalance == 0) {
      return addedExpireTime;
    }

    if (addedFrozenBalance == 0) {
      return originExpireTime;
    }

    return now +
        BigInteger.valueOf(Math.max(0, Math.min(originExpireTime - now, maxExpire)))
            .multiply(BigInteger.valueOf(originFrozenBalance))
            .add(BigInteger.valueOf(Math.max(0, Math.min(addedExpireTime - now, maxExpire)))
                .multiply(BigInteger.valueOf(addedFrozenBalance)))
            .divide(BigInteger.valueOf(Math.addExact(originFrozenBalance, addedFrozenBalance)))
            .longValue();
  }

  // TODO: 2021/3/19 即使取cache也会发生反序列化，对性能的影响，某些index capsule可以待处理完毕后再写入缓存
  private void removeOrInsertDelegatedIndexAndUpdate(byte[] accountAddr, byte[] removeAddr,
      byte[] insertAddr, ListType listType) {
    DelegatedResourceAccountIndexCapsule indexCapsule = delegatedResourceAccountIndexStore
        .get(accountAddr);
    if (indexCapsule == null) {
      indexCapsule = new DelegatedResourceAccountIndexCapsule(ByteString.copyFrom(accountAddr));
    }
    List<ByteString> list = new ArrayList<>(listType == ListType.FROM_LIST ?
        indexCapsule.getFromAccountsList() : indexCapsule.getToAccountsList());
    if (removeAddr != null) {
      list.remove(ByteString.copyFrom(removeAddr));
    }
    if (insertAddr != null && !list.contains(ByteString.copyFrom(insertAddr))) {
      list.add(ByteString.copyFrom(insertAddr));
    }
    if (listType == ListType.FROM_LIST) {
      indexCapsule.setAllFromAccounts(list);
    } else {
      indexCapsule.setAllToAccounts(list);
    }
    delegatedResourceAccountIndexStore.put(accountAddr, indexCapsule);
  }

  private enum ListType {
    FROM_LIST, TO_LIST
  }
}
