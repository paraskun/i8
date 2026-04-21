package org.blab.oss.i8.graph;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.blab.oss.i8.Node;
import org.blab.oss.i8.codec.ReflectCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
public class GraphController {
  @Autowired private Gson gson;
  @Autowired private Graph graph;
  @Autowired private KafkaProducer<Integer, byte[]> producer;
  @Autowired private KafkaConsumer<Integer, byte[]> consumer;

  @PutMapping("/sync")
  public Graph.Meta sync(@RequestBody Graph.Meta meta) throws Exception {
    return graph.sync(meta);
  }

  @GetMapping("/node/{id}")
  @SuppressWarnings("unchecked")
  public <T> List<T> monitorNode(@PathVariable("id") Integer id) {
    var n = (Node<?, ?, T>) graph.getNodes().get(id);
    var t = String.format("i8.node.%d.stdout", id);

    consumer.subscribe(Set.of(t));

    try (var codec = new ReflectCodec<>(n.getStdOutClass())) {
      return consumer.poll(Duration.ofSeconds(1))
              .records(new TopicPartition(t, 0))
              .stream()
              .map(r -> codec.deserialize(r.value()))
              .toList();
    }
  }

  @PutMapping("/node/{id}")
  @SuppressWarnings("unchecked")
  public <T> void updateNode(@PathVariable("id") Integer id, @RequestBody String body) {
    var n = (Node<?, T, ?>) graph.getNodes().get(id);
    var b = gson.fromJson(body, n.getStdInClass());
    var t = String.format("i8.node.%d.stdin", id);

    log.info("Update node {}: {}", id, b);

    try (var codec = new ReflectCodec<>(n.getStdInClass())) {
      producer.send(new ProducerRecord<>(t, 0, 0, codec.serialize(b)));
      producer.flush();
    }
  }
}
