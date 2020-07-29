package org.tron.api;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.validation.Valid;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.NativeWebRequest;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.StringUtil;
import org.tron.config.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.BadItemException;
import org.tron.model.ConstructionCombineRequest;
import org.tron.model.ConstructionCombineResponse;
import org.tron.model.ConstructionDeriveRequest;
import org.tron.model.ConstructionDeriveResponse;
import org.tron.model.ConstructionHashRequest;
import org.tron.model.ConstructionHashResponse;
import org.tron.model.ConstructionMetadataRequest;
import org.tron.model.ConstructionMetadataResponse;
import org.tron.model.ConstructionPreprocessRequest;
import org.tron.model.ConstructionPreprocessResponse;
import org.tron.model.ConstructionSubmitRequest;
import org.tron.model.ConstructionSubmitResponse;
import org.tron.model.CurveType;
import org.tron.model.Error;
import org.tron.model.PublicKey;
import org.tron.model.Signature;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;

@Controller
@RequestMapping("${openapi.rosetta.base-path:}")
public class ConstructionApiController implements ConstructionApi {

  private final NativeWebRequest request;

  @Autowired
  private Wallet wallet;

  @org.springframework.beans.factory.annotation.Autowired
  public ConstructionApiController(NativeWebRequest request) {
    this.request = request;
  }

  @Override
  public Optional<NativeWebRequest> getRequest() {
    return Optional.ofNullable(request);
  }


  /**
   * POST /construction/derive : Derive an Address from a PublicKey
   * Derive returns the network-specific address associated with a public key. Blockchains that require an on-chain action to create an account should not implement this method.
   *
   * @param constructionDeriveRequest (required)
   * @return Expected response to a valid request (status code 200)
   * or unexpected error (status code 200)
   */
  @ApiOperation(value = "Derive an Address from a PublicKey", nickname = "constructionDerive", notes = "Derive returns the network-specific address associated with a public key. Blockchains that require an on-chain action to create an account should not implement this method.", response = ConstructionDeriveResponse.class, tags = {"Construction",})
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Expected response to a valid request", response = ConstructionDeriveResponse.class),
          @ApiResponse(code = 200, message = "unexpected error", response = Error.class)})
  @RequestMapping(value = "/construction/derive",
          produces = {"application/json"},
          consumes = {"application/json"},
          method = RequestMethod.POST)
  public ResponseEntity<ConstructionDeriveResponse> constructionDerive(
          @ApiParam(value = "", required = true)
          @Valid
          @RequestBody
                  ConstructionDeriveRequest constructionDeriveRequest) {

    if (getRequest().isPresent()) {
      for (MediaType mediaType : MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          PublicKey publicKey = constructionDeriveRequest.getPublicKey();
          if (CurveType.SECP256K1.equals(publicKey.getCurveType())) {
            String hexBytes = publicKey.getHexBytes();
            byte[] address = Hash.computeAddress(ByteArray.fromHexString(hexBytes));
            ConstructionDeriveResponse response = new ConstructionDeriveResponse();
            response.address(StringUtil.encode58Check(address));
            return new ResponseEntity<>(response, HttpStatus.OK);
          }
          break;
        }
      }
    }
    return new ResponseEntity<>(HttpStatus.valueOf(200));
  }

  /**
   * POST /construction/preprocess : Create a Request to Fetch Metadata
   * Preprocess is called prior to &#x60;/construction/payloads&#x60; to construct a request for any metadata that is needed for transaction construction given (i.e. account nonce). The request returned from this method will be used by the caller (in a different execution environment) to call the &#x60;/construction/metadata&#x60; endpoint.
   *
   * @param constructionPreprocessRequest (required)
   * @return Expected response to a valid request (status code 200)
   * or unexpected error (status code 200)
   */
  @ApiOperation(value = "Create a Request to Fetch Metadata", nickname = "constructionPreprocess", notes = "Preprocess is called prior to `/construction/payloads` to construct a request for any metadata that is needed for transaction construction given (i.e. account nonce). The request returned from this method will be used by the caller (in a different execution environment) to call the `/construction/metadata` endpoint.", response = ConstructionPreprocessResponse.class, tags = {"Construction",})
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Expected response to a valid request", response = ConstructionPreprocessResponse.class),
          @ApiResponse(code = 200, message = "unexpected error", response = Error.class)})
  @RequestMapping(value = "/construction/preprocess",
          produces = {"application/json"},
          consumes = {"application/json"},
          method = RequestMethod.POST)
  public ResponseEntity<ConstructionPreprocessResponse> constructionPreprocess(@ApiParam(value = "", required = true) @Valid @RequestBody ConstructionPreprocessRequest constructionPreprocessRequest) {
    getRequest().ifPresent(request -> {
      for (MediaType mediaType : MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          String exampleString = "{ \"options\" : \"{}\" }";
          ApiUtil.setExampleResponse(request, "application/json", exampleString);
          break;
        }
      }
    });
    return new ResponseEntity<>(HttpStatus.valueOf(200));

  }

  /**
   * POST /construction/metadata : Get Metadata for Transaction Construction
   * Get any information required to construct a transaction for a specific network. Metadata returned here could be a recent hash to use, an account sequence number, or even arbitrary chain state. The request used when calling this endpoint is often created by calling &#x60;/construction/preprocess&#x60; in an offline environment. It is important to clarify that this endpoint should not pre-construct any transactions for the client (this should happen in &#x60;/construction/payloads&#x60;). This endpoint is left purposely unstructured because of the wide scope of metadata that could be required.
   *
   * @param constructionMetadataRequest (required)
   * @return Expected response to a valid request (status code 200)
   * or unexpected error (status code 200)
   */
  @ApiOperation(value = "Get Metadata for Transaction Construction", nickname = "constructionMetadata", notes = "Get any information required to construct a transaction for a specific network. Metadata returned here could be a recent hash to use, an account sequence number, or even arbitrary chain state. The request used when calling this endpoint is often created by calling `/construction/preprocess` in an offline environment. It is important to clarify that this endpoint should not pre-construct any transactions for the client (this should happen in `/construction/payloads`). This endpoint is left purposely unstructured because of the wide scope of metadata that could be required.", response = ConstructionMetadataResponse.class, tags = {"Construction",})
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Expected response to a valid request", response = ConstructionMetadataResponse.class),
          @ApiResponse(code = 200, message = "unexpected error", response = Error.class)})
  @RequestMapping(value = "/construction/metadata",
          produces = {"application/json"},
          consumes = {"application/json"},
          method = RequestMethod.POST)
  public ResponseEntity<ConstructionMetadataResponse> constructionMetadata(@ApiParam(value = "", required = true) @Valid @RequestBody ConstructionMetadataRequest constructionMetadataRequest) {
    getRequest().ifPresent(request -> {
      for (MediaType mediaType : MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          String exampleString = "{ \"metadata\" : { \"account_sequence\" : 23, \"recent_block_hash\" : \"0x52bc44d5378309ee2abf1539bf71de1b7d7be3b5\" } }";
          ApiUtil.setExampleResponse(request, "application/json", exampleString);
          break;
        }
      }
    });
    return new ResponseEntity<>(HttpStatus.valueOf(200));

  }

  /**
   * POST /construction/combine : Create Network Transaction from Signatures
   * Combine creates a network-specific transaction from an unsigned transaction and
   * an array of provided signatures. The signed transaction returned from this method
   * will be sent to the &#x60;/construction/submit&#x60; endpoint by the caller.
   *
   * @param constructionCombineRequest (required)
   * @return Expected response to a valid request (status code 200)
   * or unexpected error (status code 200)
   */
  @ApiOperation(value = "Create Network Transaction from Signatures", nickname = "constructionCombine", notes = "Combine creates a network-specific transaction from an unsigned transaction and an array of provided signatures. The signed transaction returned from this method will be sent to the `/construction/submit` endpoint by the caller.", response = ConstructionCombineResponse.class, tags = {"Construction",})
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Expected response to a valid request", response = ConstructionCombineResponse.class),
          @ApiResponse(code = 200, message = "unexpected error", response = Error.class)})
  @RequestMapping(value = "/construction/combine",
          produces = {"application/json"},
          consumes = {"application/json"},
          method = RequestMethod.POST)
  public ResponseEntity<ConstructionCombineResponse> constructionCombine(@ApiParam(value = "", required = true) @Valid @RequestBody ConstructionCombineRequest constructionCombineRequest) {
    AtomicInteger statusCode = new AtomicInteger(HttpStatus.OK.value());
    getRequest().ifPresent(request -> {
      for (MediaType mediaType : MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          String returnString;
//          BalanceContract.TransferContract transferContract = BalanceContract.TransferContract.newBuilder().setAmount(10)
//                  .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString("121212a9cf")))
//                  .setToAddress(ByteString.copyFrom(ByteArray.fromHexString("232323a9cf"))).build();
//          Protocol.Transaction transactionTest = Protocol.Transaction.newBuilder().setRawData(
//                  Protocol.Transaction.raw.newBuilder().setTimestamp(DateTime.now().minusDays(4).getMillis())
//                          .setRefBlockNum(1)
//                          .addContract(
//                                  Protocol.Transaction.Contract.newBuilder().setType(Protocol.Transaction.Contract.ContractType.TransferContract)
//                                          .setParameter(Any.pack(transferContract)).build()).build())
//                  .build();
//          System.out.println(ByteArray.toHexString(transactionTest.getRawData().toByteArray()));
//          String priKey = "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
//          TransactionCapsule transactionCapsule = new TransactionCapsule(transactionTest);
//          transactionCapsule.sign(ByteArray.fromHexString(priKey));
//          System.out.println(ByteArray.toHexString(transactionCapsule.getInstance().getSignature(0).toByteArray()));
//          constructionCombineRequest.setUnsignedTransaction(ByteArray.toHexString(transactionTest.getRawData().toByteArray()));
          try {
            Protocol.Transaction.raw transaction = Protocol.Transaction.raw.parseFrom(
                    ByteArray.fromHexString(constructionCombineRequest.getUnsignedTransaction()));
            Protocol.Transaction.Builder transactionBuilder = Protocol.Transaction.newBuilder();
            transactionBuilder.setRawData(transaction);
            for (Signature signature : constructionCombineRequest.getSignatures()) {
              transactionBuilder.addSignature(ByteString.copyFrom(ByteArray.fromHexString(signature.getHexBytes())));
            }
            ConstructionCombineResponse constructionCombineResponse = new ConstructionCombineResponse();
            constructionCombineResponse.setSignedTransaction(ByteArray.toHexString(transactionBuilder.build().toByteArray()));
            returnString = JSON.toJSONString(constructionCombineResponse);
          } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            statusCode.set(HttpStatus.INTERNAL_SERVER_ERROR.value());
            Error error = Constant.INVALID_TRANSACTION_FORMAT;
            returnString = JSON.toJSONString(error);
          }

          ApiUtil.setExampleResponse(request, "application/json", returnString);
          break;
        }
      }
    });
    return new ResponseEntity<>(HttpStatus.valueOf(statusCode.get()));

  }

  /**
   * POST /construction/hash : Get the Hash of a Signed Transaction
   * TransactionHash returns the network-specific transaction hash for a signed transaction.
   *
   * @param constructionHashRequest (required)
   * @return Expected response to a valid request (status code 200)
   * or unexpected error (status code 200)
   */
  @ApiOperation(value = "Get the Hash of a Signed Transaction", nickname = "constructionHash", notes = "TransactionHash returns the network-specific transaction hash for a signed transaction.", response = ConstructionHashResponse.class, tags = {"Construction",})
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Expected response to a valid request", response = ConstructionHashResponse.class),
          @ApiResponse(code = 200, message = "unexpected error", response = Error.class)})
  @RequestMapping(value = "/construction/hash",
          produces = {"application/json"},
          consumes = {"application/json"},
          method = RequestMethod.POST)
  public ResponseEntity<ConstructionHashResponse> constructionHash(@ApiParam(value = "", required = true) @Valid @RequestBody ConstructionHashRequest constructionHashRequest) {
    AtomicInteger statusCode = new AtomicInteger(HttpStatus.OK.value());
    getRequest().ifPresent(request -> {
      for (MediaType mediaType : MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          String returnString = "{ \"transaction_hash\" : \"transaction_hash\" }";
          try {
            TransactionCapsule transaction = new TransactionCapsule(
                    ByteArray.fromHexString(constructionHashRequest.getSignedTransaction()));
            String transactionHash = transaction.getTransactionId().toString();
            ConstructionHashResponse constructionHashResponse = new ConstructionHashResponse();
            constructionHashResponse.setTransactionHash(transactionHash);
            returnString = JSON.toJSONString(constructionHashResponse);
          } catch (BadItemException e) {
            e.printStackTrace();
            statusCode.set(HttpStatus.INTERNAL_SERVER_ERROR.value());
            Error error = Constant.INVALID_TRANSACTION_FORMAT;
            returnString = JSON.toJSONString(error);
          }

          ApiUtil.setExampleResponse(request, "application/json", returnString);
          break;
        }
      }
    });
    return new ResponseEntity<>(HttpStatus.valueOf(statusCode.get()));

  }

  /**
   * POST /construction/submit : Submit a Signed Transaction
   * Submit a pre-signed transaction to the node.
   * This call should not block on the transaction being included in a block.
   * Rather, it should return immediately with an indication of whether or not
   * the transaction was included in the mempool. The transaction submission
   * response should only return a 200 status if the submitted transaction could
   * be included in the mempool. Otherwise, it should return an error.
   *
   * @param constructionSubmitRequest (required)
   * @return Expected response to a valid request (status code 200)
   * or unexpected error (status code 200)
   */
  @ApiOperation(value = "Submit a Signed Transaction", nickname = "constructionSubmit", notes = "Submit a pre-signed transaction to the node. This call should not block on the transaction being included in a block. Rather, it should return immediately with an indication of whether or not the transaction was included in the mempool. The transaction submission response should only return a 200 status if the submitted transaction could be included in the mempool. Otherwise, it should return an error.", response = ConstructionSubmitResponse.class, tags = {"Construction",})
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Expected response to a valid request",
                  response = ConstructionSubmitResponse.class),
          @ApiResponse(code = 200, message = "unexpected error", response = Error.class)})
  @RequestMapping(value = "/construction/submit",
          produces = {"application/json"},
          consumes = {"application/json"},
          method = RequestMethod.POST)
  public ResponseEntity<ConstructionSubmitResponse> constructionSubmit(
          @ApiParam(value = "", required = true) @Valid @RequestBody ConstructionSubmitRequest constructionSubmitRequest) {
    AtomicInteger statusCode = new AtomicInteger(HttpStatus.OK.value());

    getRequest().ifPresent(request -> {
      for (MediaType mediaType : MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          String returnString = "";
          Error error = new Error();
          try {
            TransactionCapsule transactionSigned = new TransactionCapsule(
                    ByteArray.fromHexString(constructionSubmitRequest.getSignedTransaction()));
            GrpcAPI.Return result = wallet.broadcastTransaction(transactionSigned.getInstance());
            if (result.getResult()) {
              String transactionHash = transactionSigned.getTransactionId().toString();
              ConstructionSubmitResponse constructionSubmitResponse = new ConstructionSubmitResponse();
              constructionSubmitResponse.getTransactionIdentifier().setHash(transactionHash);
              returnString = JSON.toJSONString(constructionSubmitResponse);
            } else {
              statusCode.set(HttpStatus.INTERNAL_SERVER_ERROR.value());
              error.setCode(result.getCodeValue());
              error.setMessage(result.getMessage().toStringUtf8());
              error.setRetriable(true);
              returnString = JSON.toJSONString(error);
            }
          } catch (BadItemException e) {
            e.printStackTrace();
            statusCode.set(HttpStatus.INTERNAL_SERVER_ERROR.value());
            error = Constant.INVALID_TRANSACTION_FORMAT;
            returnString = JSON.toJSONString(error);
          }

          ApiUtil.setExampleResponse(request, "application/json", returnString);
          break;
        }
      }
    });
    return new ResponseEntity<>(HttpStatus.valueOf(statusCode.get()));

  }
}
