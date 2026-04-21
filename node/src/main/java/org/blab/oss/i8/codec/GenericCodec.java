package org.blab.oss.i8.codec;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.*;
import org.apache.avro.util.ByteBufferInputStream;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.formats.avro.typeutils.GenericRecordAvroTypeInfo;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GenericCodec implements
        Serializer<GenericRecord>,
        Deserializer<GenericRecord>,
        DeserializationSchema<GenericRecord>,
        SerializationSchema<GenericRecord>,
        Serializable {
  private final Schema schema;

  private transient DatumWriter<GenericRecord> writer;
  private transient DatumReader<GenericRecord> reader;

  public GenericCodec(Schema schema) {
    this.schema = schema;

    writer = new GenericDatumWriter<>(schema);
    reader = new GenericDatumReader<>(schema);
  }

  @Serial
  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
  }

  @Serial
  private void readObject(ObjectInputStream in) throws Exception {
    in.defaultReadObject();

    writer = new GenericDatumWriter<>(schema);
    reader = new GenericDatumReader<>(schema);
  }

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    Serializer.super.configure(configs, isKey);
  }

  @Override
  public byte[] serialize(GenericRecord record) {
    try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
      Encoder encoder = EncoderFactory.get().binaryEncoder(stream, null);

      writer.write(record, encoder);
      encoder.flush();

      return stream.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte[] serialize(String s, GenericRecord record) {
    return serialize(record);
  }

  @Override
  public byte[] serialize(String topic, Headers headers, GenericRecord data) {
    return serialize(data);
  }

  @Override
  public void close() {
    Serializer.super.close();
  }

  @Override
  public GenericRecord deserialize(String s, byte[] data) {
    return deserialize(data);
  }

  @Override
  public GenericRecord deserialize(String topic, Headers headers, byte[] data) {
    return deserialize(data);
  }

  @Override
  public GenericRecord deserialize(String topic, Headers headers, ByteBuffer data) {
    try {
      return reader.read(null, DecoderFactory.get()
              .binaryDecoder(new ByteBufferInputStream(List.of(data)), null));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public GenericRecord deserialize(byte[] message) {
    try {
      return reader.read(null, DecoderFactory.get().binaryDecoder(message, null));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isEndOfStream(GenericRecord nextElement) {
    return false;
  }

  @Override
  public TypeInformation<GenericRecord> getProducedType() {
    return new GenericRecordAvroTypeInfo(schema);
  }

  @Override
  public void open(SerializationSchema.InitializationContext context) throws Exception {
    SerializationSchema.super.open(context);
  }

  public static class RecordBuilder {
    private final Map<String, Object> fields;

    public RecordBuilder() {
      this.fields = new TreeMap<>();
    }

    public RecordBuilder with(String k, Object v) {
      fields.put(k, v);
      return this;
    }

    public GenericRecord build(Schema schema) {
      var r = new GenericData.Record(schema);

      for (var f : fields.entrySet()) {
        r.put(f.getKey(), f.getValue());
      }

      return r;
    }
  }
}
