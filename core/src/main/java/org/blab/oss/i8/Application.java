package org.blab.oss.i8;

import com.google.gson.Gson;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Map;

@SpringBootApplication
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Bean
  public Gson gson() {
    return new Gson();
  }

  @Bean("admin")
  public Admin admin(
          @Value("${kafka.bootstrap.server}") String server
  ) {
    return Admin.create(Map.of(
            "bootstrap.servers", server
    ));
  }

  @Bean("producer")
  public KafkaProducer<Integer, byte[]> producer(
          @Value("${kafka.bootstrap.server}") String server
  ) {

    return new KafkaProducer<Integer, byte[]>(Map.of(
            "bootstrap.servers", server
    ),
            new IntegerSerializer(),
            new ByteArraySerializer());
  }

  @Bean("consumer")
  public KafkaConsumer<Integer, byte[]> consumer(
          @Value("${kafka.bootstrap.server}") String server
  ) {
    return new KafkaConsumer<>(Map.of(
            "bootstrap.servers", server,
            "group.id", "core"
    ),
            new IntegerDeserializer(),
            new ByteArrayDeserializer());
  }
}
