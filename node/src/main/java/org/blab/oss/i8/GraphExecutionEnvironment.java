package org.blab.oss.i8;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.streaming.api.datastream.DataStream;

public interface GraphExecutionEnvironment {
    <T> DataStream<T> from(String channel, DeserializationSchema<T> schema);
    <T> Sink<T> into(String channel, SerializationSchema<T> schema);
}
