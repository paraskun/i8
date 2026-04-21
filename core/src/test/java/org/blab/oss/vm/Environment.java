package org.blab.oss.vm;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.kafka.common.TopicPartition;
import org.blab.oss.i8.GraphExecutionEnvironment;

import java.util.Set;

public class Environment implements GraphExecutionEnvironment {
  private final StreamExecutionEnvironment environment;

  public Environment(StreamExecutionEnvironment environment) {
    this.environment = environment;
  }

  @Override
  public <T> DataStream<T> from(String channel, DeserializationSchema<T> schema) {
    return environment.fromSource(KafkaSource.<T>builder()
            .setBootstrapServers(System.getenv("KAFKA_BOOTSTRAP_SERVER"))
            .setPartitions(Set.of(new TopicPartition(channel, 0)))
            .setStartingOffsets(OffsetsInitializer.latest())
            .setValueOnlyDeserializer(schema)
            .build(), WatermarkStrategy.noWatermarks(), channel);
  }

  @Override
  public <T> Sink<T> into(String channel, SerializationSchema<T> schema) {
    return KafkaSink.<T>builder()
            .setBootstrapServers(System.getenv("KAFKA_BOOTSTRAP_SERVER"))
            .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                    .setTopic(channel)
                    .setValueSerializationSchema(schema)
                    .build())
            .build();
  }
}
