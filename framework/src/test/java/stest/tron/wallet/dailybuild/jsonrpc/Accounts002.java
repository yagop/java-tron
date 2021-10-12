package stest.tron.wallet.dailybuild.jsonrpc;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.grpc.ManagedChannelBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol.Block;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.JsonRpcBase;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class Accounts002 extends JsonRpcBase {
  JSONObject responseContent;
  HttpResponse response;
  String topic0 = null;
  String topic1 = null;
  String fromBlock = null;
  String toBlock = null;
  String newFilterResultIdfrom01 = null;
  String newFilterResultIdfrom02 = null;
  String blockHash = null;

  @Test(enabled = true, description = "Eth api of eth_newFilter contains nothing.")
  public void test01GetNewFilterContainNothing() {
    JsonObject paramBody = new JsonObject();
    JsonArray params = new JsonArray();
    params.add(paramBody);
    JsonObject requestBody = getJsonRpcBody("eth_newFilter", params);
    logger.info("test01GetNewFilterContainNothing_requestBody " + requestBody);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test01GetNewFilterContainNothing_responseContent" + responseContent);
    Assert.assertNotNull(responseContent.get("result"));
  }

  @Test(
      enabled = true,
      description = "Eth api of eth_newFilter contains address,fromBlock and toBlock.")
  public void test02GetNewFilterContainAddress() {
    if (blockNumForTrc20 - 10 < 0) {
      fromBlock = "0";
    } else {
      fromBlock = "0x" + Integer.toHexString(blockNumForTrc20 - 10);
    }
    toBlock = "0x" + Integer.toHexString(blockNumForTrc20 + 10);
    JsonArray addressArray = new JsonArray();
    addressArray.add(contractTrc20AddressFrom58.substring(2));
    JsonObject paramBody = new JsonObject();
    paramBody.add("address", addressArray);
    paramBody.addProperty("fromBlock", fromBlock);
    paramBody.addProperty("toBlock", toBlock);
    JsonArray params = new JsonArray();
    params.add(paramBody);
    JsonObject requestBody = getJsonRpcBody("eth_newFilter", params);
    logger.info("test02GetNewFilterContainAddress_requestBody " + requestBody);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test02GetNewFilterContainAddress_responseContent" + responseContent);
    Assert.assertNotNull(responseContent.get("result"));
    newFilterResultIdfrom01 = responseContent.getString("result");
    logger.info("test02GetNewFilterContainAddress_id:" + newFilterResultIdfrom01);
  }

  @Test(
      enabled = true,
      description = "Eth api of eth_newFilter  contains topic fromBlock and toBlock.")
  public void test03GetNewFilterContainTopic() {
    response = HttpMethed.getBlockByNum(httpFullNode,blockNumForTrc20);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("responseContent:"+responseContent);
    logger.info("blockHash:"+responseContent.getString("blockID"));

    blockHash = responseContent.getString("blockID");
    JsonArray topicArray = new JsonArray();
    topicArray.add("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef");
    JsonObject paramBody = new JsonObject();
    paramBody.add("topics", topicArray);
    paramBody.addProperty("fromBlock", fromBlock);
    paramBody.addProperty("toBlock", toBlock);
    JsonArray params = new JsonArray();
    params.add(paramBody);
    JsonObject requestBody = getJsonRpcBody("eth_newFilter", params);
    logger.info("test03GetNewFilterContainTopic_requestBody " + requestBody);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test03GetNewFilterContainTopic_responseContent" + responseContent);
    Assert.assertNotNull(responseContent.get("result"));
    newFilterResultIdfrom02 = responseContent.getString("result");
    logger.info("test03GetNewFilterContainTopic_id:" + newFilterResultIdfrom02);
  }

  @Test(enabled = true, description = "Eth api of eth_newFilter  contains topic and address.")
  public void test04GetNewFilterContainsTopicAndAddress() {

    JsonArray addressArray = new JsonArray();
    addressArray.add(contractTrc20AddressFrom58.substring(2));
    JsonArray topicArray = new JsonArray();
    topicArray.add("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef");
    JsonObject paramBody = new JsonObject();
    paramBody.add("address", addressArray);
    paramBody.add("topics", topicArray);
    paramBody.addProperty("fromBlock", fromBlock);
    paramBody.addProperty("toBlock", toBlock);
    JsonArray params = new JsonArray();
    params.add(paramBody);
    JsonObject requestBody = getJsonRpcBody("eth_newFilter", params);
    logger.info("test04GetNewFilterContainsTopicAndAddress_requestBody " + requestBody);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test04GetNewFilterContainsTopicAndAddress_responseContent" + responseContent);
    Assert.assertNotNull(responseContent.get("result"));
  }

  @Test(enabled = true, description = "Eth api of eth_newFilter only contain topic and blockHash.")
  public void test05GetNewFilterOnlyContainTopic() {
    JsonObject paramBody = new JsonObject();
    paramBody.addProperty("blockHash", blockHash);
    JsonArray topicArray = new JsonArray();
    topicArray.add("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef");
    paramBody.add("topics", topicArray);
    JsonArray params = new JsonArray();
    params.add(paramBody);
    JsonObject requestBody = getJsonRpcBody("eth_newFilter", params);
    logger.info("test05GetNewFilterOnlyContainTopic_requestBody " + requestBody);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test05GetNewFilterOnlyContainTopic_responseContent" + responseContent);
    Assert.assertNotNull(responseContent.get("result"));
  }

  @Test(enabled = true, description = "Eth api of eth_newFilter which only contains blockHash.")
  public void test06GetNewFilterHasOnlyBlockHash() {

    response = HttpMethed.getNowBlock(httpFullNode);
    responseContent = HttpMethed.parseResponseContent(response);
    String blockHash = responseContent.getString("blockID");
    JsonObject paramBody = new JsonObject();
    paramBody.addProperty("blockHash", blockHash);
    JsonArray params = new JsonArray();
    params.add(paramBody);
    JsonObject requestBody = getJsonRpcBody("eth_newFilter", params);
    logger.info("test06GetNewFilterHasOnlyBlockHash_requestBody " + requestBody);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test06GetNewFilterHasOnlyBlockHash_responseContent" + responseContent);
    Assert.assertNotNull(responseContent.get("result"));
  }

  @Test(enabled = true, description = "Eth api of eth_newFilter check new  and after block.")
  public void test07GetNewFilterCheckNewBlock() {
    JsonObject paramBody = new JsonObject();
    JsonArray topicArray = new JsonArray();
    topicArray.add("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef");
    paramBody.add("topics", topicArray);
    JsonArray params = new JsonArray();
    params.add(paramBody);
    JsonObject requestBody = getJsonRpcBody("eth_newFilter", params);
    logger.info("test07GetNewFilterCheckNewBlock_requestBody " + requestBody);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test07GetNewFilterCheckNewBlock_responseContent" + responseContent);
    Assert.assertNotNull(responseContent.get("result"));
  }

  @Test(enabled = true, description = "Eth api of eth_newBlockFilter")
  public void test08GetEthNewBlockFilter() {

    JsonArray params = new JsonArray();
    JsonObject requestBody = getJsonRpcBody("eth_newBlockFilter", params);
    logger.info("test08GetEthNewBlockFilter_requestBody " + requestBody);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test08GetEthNewBlockFilter_responseContent" + responseContent);
    Assert.assertNotNull(responseContent.get("result"));
  }

  @Test(enabled = true, description = "Eth api of eth_getFilterChanges has less 20 elements.")
  public void test09GetFilterChanges() {

    JsonArray params = new JsonArray();
    params.add(NewFilterId);
    JsonObject requestBody = getJsonRpcBody("eth_getFilterChanges", params);
    logger.info("test09GetFilterChanges_requestBody: " + requestBody);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test09GetFilterChanges_responseContent:" + responseContent);
    Assert.assertNotNull(responseContent.get("result"));
  }

  @Test(
      enabled = true,
      description = "Eth api of eth_getLogs  contains address ,fromBlock and toBlock.")
  public void test10GetLogsOnlyContainAddress() {
    JsonArray addressArray = new JsonArray();
    addressArray.add(contractTrc20AddressFrom58.substring(2));
    JsonObject paramBody = new JsonObject();
    paramBody.add("address", addressArray);
    paramBody.addProperty("fromBlock", "0x" + (Integer.toHexString(blockNumForTrc20 - 10)));
    paramBody.addProperty("toBlock", "0x" + (Integer.toHexString(blockNumForTrc20 + 10)));
    JsonArray params = new JsonArray();
    params.add(paramBody);
    JsonObject requestBody = getJsonRpcBody("eth_getLogs", params);
    logger.info("test10GetLogsOnlyContainAddress_requestBody：" + requestBody);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test10GetLogsOnlyContainAddress_responseContent：" + responseContent);
    Assert.assertNotNull(responseContent.get("result"));
    String address =
        responseContent.getJSONArray("result").getJSONObject(0).getString("address").substring(2);
    Assert.assertEquals(address, contractTrc20AddressFrom58.substring(2));
    topic0 = responseContent.getJSONArray("result").getJSONObject(0).getString("topic");
  }

  @Test(enabled = true, description = "Eth api of eth_getLogs both contains topic and address.")
  public void test11GetLogsContainsTopicAndAddress() {
    JsonArray topicArray = new JsonArray();
    topicArray.add(topic0);
    JsonObject paramBody = new JsonObject();
    paramBody.add("topics", topicArray);
    paramBody.addProperty("fromBlock", "0x" + (Integer.toHexString(blockNumForTrc20 - 10)));
    paramBody.addProperty("toBlock", "0x" + (Integer.toHexString(blockNumForTrc20 + 10)));
    JsonArray params = new JsonArray();
    params.add(paramBody);
    JsonObject requestBody = getJsonRpcBody("eth_getLogs", params);
    logger.info("test11GetLogsContainsTopicAndAddress_requestBody " + requestBody);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test11GetLogsContainsTopicAndAddress_responseContent" + responseContent);
    Assert.assertNotNull(responseContent.get("result"));
    String topicFromResult =
        responseContent.getJSONArray("result").getJSONObject(0).getString("topic");
    Assert.assertEquals(topicFromResult, topic0);
  }

  @Test(enabled = true, description = "Eth api of eth_getFilterLogs .")
  public void test12GetFilterLogsContainsAddress() {

    JsonArray params = new JsonArray();
    params.add(newFilterResultIdfrom01);
    JsonObject requestBody = getJsonRpcBody("eth_getFilterLogs", params);
    logger.info("test12GetFilterLogsContainsAddress_requestBody " + requestBody);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test12GetFilterLogsContainsAddress_responseContent" + responseContent);
    Assert.assertNotNull(responseContent.get("result"));
  }

  @Test(enabled = true, description = "Eth api of eth_getFilterLogs .")
  public void test13GetFilterLogsContainsTopic() {

    JsonArray params = new JsonArray();
    params.add(newFilterResultIdfrom02);
    JsonObject requestBody = getJsonRpcBody("eth_getFilterLogs", params);
    logger.info("test13GetFilterLogsContainsTopic_requestBody " + requestBody);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test13GetFilterLogsContainsTopic_responseContent" + responseContent);
    Assert.assertNotNull(responseContent.get("result"));
  }

  @Test(
      enabled = true,
      description =
          "Eth api of eth_uninstallFilter which method is eth_newFilter and params has one element ")
  public void test14EthUninstallFilter() {
    // create ID
    JsonArray addressArray = new JsonArray();
    addressArray.add(contractTrc20AddressFrom58.substring(2));
    JsonObject paramBody = new JsonObject();
    paramBody.add("address", addressArray);
    paramBody.addProperty("fromBlock", "0x1f8b6a7");
    paramBody.addProperty("toBlock", "0x1f8b6a7");
    JsonArray params = new JsonArray();
    params.add(paramBody);
    JsonObject requestBody = getJsonRpcBody("eth_newFilter", params);
    logger.info("test14_newfilter " + requestBody);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test14_newfilter_responseContentr" + responseContent);
    String EthNewFilterResult = responseContent.get("result").toString();
    Assert.assertNotNull(responseContent.get("result"));

    // verify ID invalid

    // first time
    params = new JsonArray();
    params.add(responseContent.get("result").toString());
    requestBody = getJsonRpcBody("eth_uninstallFilter", params);
    logger.info("test14_eth_uninstallFilter " + requestBody);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test14_eth_uninstallFilter_responseContentr_first" + responseContent);
    Assert.assertEquals(responseContent.get("result"), true);
    // second time
    logger.info("test14_eth_uninstallFilter_second " + requestBody);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test14_eth_uninstallFilter_responseContentr_second " + responseContent);
    Assert.assertEquals(
        responseContent.getJSONObject("error").getString("message"), "filter not found");

    // query getFilterChanges to verify ID has invalid
    params = new JsonArray();
    params.add(EthNewFilterResult);
    requestBody = getJsonRpcBody("getFilterChanges", params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test14EthUninstallFilter_responseContent" + responseContent);
    String expect_result = "{\"code\":-32000,\"data\":\"{}\",\"message\":\"filter not found\"}";
    Assert.assertEquals(responseContent.get("error").toString(), expect_result);
  }

  @Test(
      enabled = true,
      description =
          "Eth api of eth_uninstallFilter which method is eth_newBlockFilter and params has one element ")
  public void test15EthUninstallFilter() {
    // create ID

    JsonArray params = new JsonArray();
    JsonObject requestBody = getJsonRpcBody("eth_newBlockFilter", params);
    logger.info("test15EthUninstallFilter_newBlockFilter " + requestBody);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test15EthUninstallFilter_newBlockFilter_responseContentr" + responseContent);
    String EthNewBlockFilterResult = responseContent.get("result").toString();
    Assert.assertNotNull(responseContent.get("result"));

    // verify ID invalid
    // first time
    params = new JsonArray();
    params.add(responseContent.get("result").toString());
    requestBody = getJsonRpcBody("eth_uninstallFilter", params);
    logger.info("test15_eth_uninstallFilter " + requestBody);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test15_eth_uninstallFilter_responseContentr_first" + responseContent);
    String EthNewFilterRespouse = responseContent.get("result").toString();
    Assert.assertEquals(responseContent.get("result"), true);
    // second time
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test15_eth_uninstallFilter_responseContentr_second" + responseContent);
    Assert.assertEquals(
        responseContent.getJSONObject("error").getString("message"), "filter not found");
    // query getFilterChanges to verify ID has invalid
    params = new JsonArray();
    params.add(EthNewBlockFilterResult);
    requestBody = getJsonRpcBody("getFilterChanges", params);
    response = getJsonRpc(jsonRpcNode, requestBody);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("test15EthUninstallFilter_responseContent" + responseContent);
    String expect_result = "{\"code\":-32000,\"data\":\"{}\",\"message\":\"filter not found\"}";
    Assert.assertEquals(responseContent.get("error").toString(), expect_result);
  }
}
