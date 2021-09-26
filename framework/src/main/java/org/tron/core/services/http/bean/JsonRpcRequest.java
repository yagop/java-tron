package org.tron.core.services.http.bean;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;

@Data
public class JsonRpcRequest {

  private String jsonrpc;

  private String method;

  private JSONArray params;

  private Object id;

  public String getParam(int index) {
    if (params == null || params.size() <= index) {
      return "";
    } else {
      return params.getString(index);
    }
  }

  @Override
  public String toString() {
    return "JsonRpcArgs{" +
        "jsonrpc='" + jsonrpc + '\'' +
        ", method='" + method + '\'' +
        ", params=" + params +
        ", id=" + id +
        '}';
  }
}
