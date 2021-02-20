package org.tron.core.db2.common;

import com.google.protobuf.GeneratedMessageV3;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Arrays;

@EqualsAndHashCode(exclude = "operator")
public final class InstanceValue<U extends GeneratedMessageV3> {

  @Getter
  final private Operator operator;
  final private U data;

  private InstanceValue(Operator operator, U data) {
    this.operator = operator;
    this.data = data;
  }

  public static <U extends GeneratedMessageV3> InstanceValue<U> copyOf(Operator operator, U data) {
    return new InstanceValue<>(operator, data);
  }

  public static <U extends GeneratedMessageV3> InstanceValue<U> of(Operator operator, U data) {
    return new InstanceValue<>(operator, data);
  }

  public byte[] encode() {
    if (data == null) {
      return new byte[]{operator.getValue()};
    }

    byte[] d = data.toByteArray();
    byte[] r = new byte[1 + d.length];
    r[0] = operator.getValue();
    System.arraycopy(d, 0, r, 1, d.length);
    return r;
  }

  public byte[] getBytes() {
    return data.toByteArray();
  }

  public U getInstance() {
    return data;
  }

  public enum Operator {
    CREATE((byte) 0),
    MODIFY((byte) 1),
    DELETE((byte) 2),
    PUT((byte) 3);

    @Getter
    private byte value;

    Operator(byte value) {
      this.value = value;
    }

    static Operator valueOf(byte b) {
      switch (b) {
        case 0:
          return Operator.CREATE;
        case 1:
          return Operator.MODIFY;
        case 2:
          return Operator.DELETE;
        case 3:
          return Operator.PUT;
        default:
          return null;
      }
    }
  }
}
