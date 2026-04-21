package org.blab.oss.i8.codec;

import com.google.gson.Gson;
import org.apache.flink.api.common.serialization.SerializationSchema;

import java.nio.charset.StandardCharsets;

public class GsonCodec<T> implements SerializationSchema<T> {
  @Override
  public byte[] serialize(T element) {
    return new Gson().toJson(element).getBytes(StandardCharsets.UTF_8);
  }
}
