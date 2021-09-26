package org.tron.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.tron.common.crypto.Hash;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.runtime.ProgramResult;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.db.TransactionContext;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.utils.TransactionUtil;
import org.tron.core.vm.EnergyCost;
import org.tron.core.vm.PrecompiledContracts;
import org.tron.core.vm.VM;
import org.tron.core.vm.VMConstant;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.invoke.ProgramInvoke;
import org.tron.core.vm.program.invoke.ProgramInvokeImpl;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.SmartContractOuterClass;
import org.tron.protos.contract.SmartContractOuterClass.EthTransaction;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

import static org.apache.commons.lang3.ArrayUtils.getLength;
import static org.tron.core.vm.utils.MUtil.transfer;

@Slf4j(topic = "actuator")
public class EthTxActuator implements Actuator2 {

  private static final BigInteger G = BigInteger.valueOf(10).pow(9);
  private static final BigInteger D = BigInteger.valueOf(10).pow(12);

  private TransactionContext context;
  private BlockCapsule block;
  private Transaction tronTx;
  private EthTransaction ethTx;
  private boolean isConstant;

  private long gas;
  private long value;

  private Repository rep;

  private Program program;
  private ProgramResult result = new ProgramResult();

  public EthTxActuator(boolean isConstant) {
    this.isConstant = isConstant;
  }

  @Override
  public void validate(Object object) throws ContractValidateException {
    TransactionContext context = (TransactionContext) object;
    if (Objects.isNull(context)) {
      throw new RuntimeException("TransactionContext is null");
    }

    ConfigLoader.load(context.getStoreFactory());

    this.context = context;
    this.block = context.getBlockCap();
    this.tronTx = context.getTrxCap().getInstance();
    try {
      this.ethTx = tronTx.getRawData().getContract(0).getParameter()
          .unpack(EthTransaction.class);
    } catch (InvalidProtocolBufferException e) {
      throw new ContractValidateException("");
    }
    this.rep = RepositoryImpl.createRoot(context.getStoreFactory());

    this.gas = toLong(ethTx.getGas());
    BigInteger gasCost = new BigInteger(1, ethTx.getGasPrice().toByteArray())
        .divide(G).multiply(BigInteger.valueOf(gas));
    BigInteger maxFeeLimit = BigInteger.valueOf(rep.getDynamicPropertiesStore().getMaxFeeLimit());
    if (gasCost.compareTo(maxFeeLimit) > 0) {
      throw new ContractValidateException("Exceed max fee limit.");
    }

    this.value = new BigInteger(1, ethTx.getValue().toByteArray()).divide(D).longValue();
    BigInteger totalCost = gasCost.add(BigInteger.valueOf(value));
    AccountCapsule from = rep.getAccount(ethTx.getOwnerAddress().toByteArray());
    BigInteger senderBalance = BigInteger.valueOf(from.getBalance()).multiply(D);
    if (senderBalance.compareTo(totalCost) < 0) {
      throw new ContractValidateException("Sender balance not cover gas fee and value.");
    }

    prepare();
  }

  @Override
  public void execute(Object ignored) throws ContractExeException {
    if (block != null && block.generatedByMyself && block.hasWitnessSignature()
        && Transaction.Result.contractResult.OUT_OF_TIME
          == TransactionUtil.getContractRet(tronTx)) {
//      ProgramResult result = program.getResult();
//      program.spendAllEnergy();

      Program.OutOfTimeException e = Program.Exception.alreadyTimeOut();
      result.setRuntimeError(e.getMessage());
      result.setException(e);
      throw e;
    }

    if (this.program != null) {
      VM vm = new VM();
      vm.play(program);
      result = program.getResult();

      if (isContractCreation() && !result.isRevert()) {
        byte[] code = program.getResult().getHReturn();

        long saveCodeEnergy = (long) getLength(code) * EnergyCost.getInstance().getCREATE_DATA();
        long afterSpend = program.getEnergyLimitLeft().longValue() - saveCodeEnergy;
        if (afterSpend < 0) {
          if (null == result.getException()) {
            result.setException(Program.Exception
                .notEnoughSpendEnergy("save just created contract code",
                    saveCodeEnergy, program.getEnergyLimitLeft().longValue()));
          }
        } else {
          result.spendEnergy(saveCodeEnergy);
          rep.saveCode(program.getContractAddress().getNoLeadZeroesData(), code);
        }
      }

      if (result.getException() != null || result.isRevert()) {
        result.getDeleteAccounts().clear();
        result.getLogInfoList().clear();
        result.rejectInternalTransactions();

        if (result.getException() != null) {
          if (!(result.getException() instanceof Program.TransferException)) {
            program.spendAllEnergy();
          }
          result.setRuntimeError(result.getException().getMessage());
          throw result.getException();
        } else {
          result.setRuntimeError("REVERT opcode executed");
        }
      } else {
        rep.commit();
      }
    } else {
      rep.commit();
    }
    context.setProgramResult(result);
    System.out.println(Hex.toHexString(result.getContractAddress()));
  }

  private void prepare() throws ContractValidateException {
    // increase nonce
    byte[] fromAddress = ethTx.getOwnerAddress().toByteArray();
    AccountCapsule accountCapsule = rep.getAccount(fromAddress);
    rep.updateAccount(fromAddress, accountCapsule.increaseNonce());

    if (isContractCreation()) {
      create();
    } else {
      call();
    }
  }

  private void create() throws ContractValidateException {
    byte[] contractAddress = generateAddress(ethTx.getOwnerAddress().toByteArray(),
        ethTx.getNonce().toByteArray());

    if (rep.getContract(contractAddress) != null) {
      throw new ContractValidateException("Contract already exist.");
    }

    if (ethTx.getData().isEmpty()) {
      // TODO implement this
    } else {
      createProgram(contractAddress, ethTx.getData().toByteArray(), true);
    }

    program.getResult().setContractAddress(contractAddress);
    SmartContractOuterClass.SmartContract contract =
        SmartContractOuterClass.SmartContract.newBuilder()
            .setContractAddress(ByteString.copyFrom(contractAddress))
            .setOriginAddress(ethTx.getOwnerAddress()).setVersion(1).build();
    rep.createContract(contractAddress, new ContractCapsule(contract));
    rep.createAccount(contractAddress, "", Protocol.AccountType.Contract);
    transfer(rep, ethTx.getOwnerAddress().toByteArray(), contractAddress, value);
  }

  private void call() throws ContractValidateException {
    byte[] toAddress = tronAddress(ethTx.getTo().toByteArray());
    PrecompiledContracts.PrecompiledContract precompiledContract =
        PrecompiledContracts.getContractForAddress(new DataWord(toAddress));

    if (precompiledContract != null) {
      // TODO implement this
    } else {
      byte[] code = rep.getCode(toAddress);
      if (!empty(code)) {
        createProgram(toAddress, code, false);
      } else {
        result.spendEnergy(21000);
      }
    }

    if (rep.getAccount(toAddress) == null) {
      rep.createNormalAccount(toAddress);
    }
    transfer(rep, ethTx.getOwnerAddress().toByteArray(), toAddress, value);
  }

  private void createProgram(byte[] contractAddress, byte[] code, boolean isCreate)
      throws ContractValidateException {

    long maxCpuTimeOfOneTx = rep.getDynamicPropertiesStore()
        .getMaxCpuTimeOfOneTx() * VMConstant.ONE_THOUSAND;
    long thisTxCPULimitInUs = (long) (maxCpuTimeOfOneTx * 1.0);
    long vmStartInUs = System.nanoTime() / VMConstant.ONE_THOUSAND;
    long vmShouldEndInUs = vmStartInUs + thisTxCPULimitInUs;

    Protocol.BlockHeader header;
    if (context.getBlockCap() == null) {
      header = new BlockCapsule(Protocol.Block.newBuilder().build()).getInstance().getBlockHeader();
    } else {
      header = context.getBlockCap().getInstance().getBlockHeader();
    }
    ProgramInvoke invoke = new ProgramInvokeImpl(contractAddress,
        ethTx.getOwnerAddress().toByteArray(),
        ethTx.getOwnerAddress().toByteArray(),
        0, toLong(ethTx.getValue()), 0, 0,
        ethTx.getData().toByteArray(),
        header.getRawData().getParentHash().toByteArray(),
        header.getRawData().getWitnessAddress().toByteArray(),
        header.getRawData().getTimestamp() / 1000,
        header.getRawData().getNumber(),
        rep, vmStartInUs, vmShouldEndInUs, toLong(ethTx.getGas()));

    this.program = new Program(code, invoke,
        new InternalTransaction(tronTx, InternalTransaction.TrxType.TRX_UNKNOWN_TYPE),
        VMConfig.getInstance());

    byte[] txId = TransactionUtil.getTransactionId(tronTx).getBytes();
    this.program.setRootTransactionId(txId);
  }

  private boolean isContractCreation() {
    return ethTx.getTo().isEmpty();
  }

   private boolean empty(byte[] data) {
     return data == null || data.length == 0;
   }

  private byte[] generateAddress(byte[] from, byte[] nonce) {
    byte[] data = new byte[from.length + nonce.length];
    System.arraycopy(from, 0, data, 0, from.length);
    System.arraycopy(nonce, 0, data, from.length, nonce.length);
    return tronAddress(Arrays.copyOfRange(Hash.sha3(data), 12, 32));
  }

  private long toLong(ByteString data) {
    return new BigInteger(1, data.toByteArray()).longValue();
  }

  private byte[] tronAddress(ByteString ethAddress) {
    return tronAddress(ethAddress.toByteArray());
  }

  private byte[] tronAddress(byte[] ethAddress) {
    byte[] tronAddress = new byte[21];
    tronAddress[0] = 0x41;
    System.arraycopy(ethAddress, 0, tronAddress, 1, 20);
    return tronAddress;
  }
}
