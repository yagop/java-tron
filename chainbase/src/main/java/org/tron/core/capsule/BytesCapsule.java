package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import org.tron.protos.Protocol;

public class BytesCapsule implements ProtoCapsule<Protocol.ByteArray> {

  private Protocol.ByteArray byteArray;

  public BytesCapsule(byte[] bytes) {
    this.byteArray = Protocol.ByteArray.newBuilder().setData(ByteString.copyFrom(bytes)).build();
  }

  public BytesCapsule(Protocol.ByteArray bytes) {
    this.byteArray = bytes;
  }

  @Override
  public byte[] getData() {
    return byteArray.getData().toByteArray();
  }

  @Override
  public Protocol.ByteArray getInstance() {
    return byteArray;
  }
}
