package org.blab.oss.i8.codec;


import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.util.ByteBufferInputStream;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class ReflectCodec<T> implements
        Serializer<T>,
        Deserializer<T>,
        SerializationSchema<T>,
        DeserializationSchema<T>,
        Serializable {
  private final Class<T> clazz;

  private transient ReflectDatumReader<T> reader;
  private transient ReflectDatumWriter<T> writer;

  public ReflectCodec(Class<T> clazz) {
    this.clazz = clazz;

    reader = new ReflectDatumReader<T>(clazz);
    writer = new ReflectDatumWriter<T>(clazz);
  }

  @Serial
  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
  }

  @Serial
  private void readObject(ObjectInputStream in) throws Exception {
    in.defaultReadObject();

    reader = new ReflectDatumReader<>(clazz);
    writer = new ReflectDatumWriter<>(clazz);
  }

  @Override
  public T deserialize(String s, byte[] data) {
    return deserialize(data);
  }

  @Override
  public T deserialize(String topic, Headers headers, byte[] data) {
    return deserialize(data);
  }

  @Override
  public T deserialize(String topic, Headers headers, ByteBuffer data) {
    try {
      return reader.read(null, DecoderFactory.get()
              .binaryDecoder(new ByteBufferInputStream(List.of(data)), null));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
  }

  @Override
  public byte[] serialize(String s, T t) {
    return serialize(t);
  }

  @Override
  public byte[] serialize(String topic, Headers headers, T data) {
    return serialize(data);
  }

  @Override
  public void close() {
  }

  @Override
  public T deserialize(byte[] data) {
    try {
      return reader.read(null, DecoderFactory.get()
              .binaryDecoder(data, null));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isEndOfStream(T nextElement) {
    return false;
  }

  @Override
  public byte[] serialize(T t) {
    try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
      var encoder = EncoderFactory.get().binaryEncoder(stream, null);

      writer.write(t, encoder);
      encoder.flush();

      return stream.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public TypeInformation<T> getProducedType() {
    return TypeInformation.of(clazz);
  }
}
