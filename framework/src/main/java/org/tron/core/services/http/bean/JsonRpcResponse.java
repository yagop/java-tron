package org.tron.core.services.http.bean;

import lombok.Data;

@Data
public class JsonRpcResponse {

  private Object id;

  private String jsonrpc;

  private Object result;

  private Object error;

  public JsonRpcResponse(JsonRpcRequest request) {
    this.id = request.getId();
    this.jsonrpc = request.getJsonrpc();
  }
}
