package org.blab.oss.i8;

import lombok.*;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.blab.oss.i8.codec.GenericCodec;

import java.io.Serializable;

@Getter
@Builder
public class Port implements Serializable {
  private transient final GraphExecutionEnvironment environment;

  private String type;
  private String channel;
  private Schema schema;

  public void assign(String channel) {
    this.channel = channel;
  }

  public Meta getMeta() {
    return new Meta(type, channel, schema.toString());
  }

  public DataStream<GenericRecord> asSource() {
    return environment.from(channel, new GenericCodec(schema));
  }

  public Sink<GenericRecord> asSink() {
    return environment.into(channel, new GenericCodec(schema));
  }

  @Getter @ToString
  @AllArgsConstructor
  public static class Meta {
    private String type;
    private String channel;
    private String schema;
  }
}
