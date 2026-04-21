package org.blab.oss.i8.graph;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.blab.oss.i8.Node;
import org.blab.oss.i8.boot.Job;
import org.blab.oss.i8.flink.FlinkClient;
import org.blab.oss.i8.plugin.PluginController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class Graph {
  @Autowired private PluginController controller;
  @Autowired private Admin admin;
  @Autowired private FlinkClient flink;

  @Value("${node.path}") String nodePath;

  @Getter private Meta meta;
  @Getter private ArrayList<Node<?, ?, ?>> nodes;

  private final List<Job> jobs = new LinkedList<>();

  public Graph() {
    this.meta = new Meta();
  }

  public Meta sync(Meta meta) throws Exception {
    if (meta.getNodes() == null) {
      return this.meta;
    }

    this.meta = meta;
    log.info("Sync: {}", meta);
    update();

    return this.meta;
  }

  public void update() throws Exception {
    jobs.forEach((job) -> flink.terminate(job));
    jobs.clear();

    nodes = new ArrayList<>(meta.getNodes().size());

    var topics = new HashSet<String>();

    for (var nm : meta.getNodes()) {
      var node = controller.load(controller.read(nm.getType()))
              .getClazz()
              .getConstructor(Node.Meta.class)
              .newInstance(nm);

      node.build();
      nodes.add(node);
      topics.add(String.format("i8.node.%d.stdin", nm.getId()));
      topics.add(String.format("i8.node.%d.stdout", nm.getId()));
    }

    var sources = new ArrayList<ArrayList<Link.End>>(nodes.size());

    for (var node : nodes) {
      sources.add(new ArrayList<>(Collections.nCopies(node.numberOfPorts(), null)));
    }

    for (var link : meta.getLinks()) {
      sources
              .get(link.getTarget().getNode())
              .set(link.getTarget().getPort(), link.getSource());
    }

    for (int i = 0; i < nodes.size(); ++i) {
      for (int j = 0; j < nodes.get(i).numberOfPorts(); ++j) {
        assign(sources, i, j, topics);
      }
    }

    admin.createTopics(topics
            .stream()
            .map(t -> new NewTopic(t, 1, (short) 1))
            .toList());

    for (var node : nodes) {
      for (int i = 0; i < node.numberOfPorts(); ++i) {
        node.getMeta()
                .getPorts()
                .set(i, node.port(i).getMeta());
      }

      var job = Job.Description.builder()
              .nodes(List.of(node.getMeta()))
              .build();

      log.info("Submit: {}", job);
      jobs.add(flink.submit(job));
    }
  }

  private void assign(ArrayList<ArrayList<Link.End>> sources, int i, int j, Set<String> topics) {
    var cur = nodes.get(i).port(j);

    if (cur.getChannel() != null) {
      return;
    }

    var idx = sources.get(i).get(j);

    if (idx == null) {
      var topic = String.format("i8.port.%d.%d", i, j);

      cur.assign(topic);
      topics.add(topic);

      return;
    }

    var src = nodes.get(idx.getNode()).port(idx.getPort());

    if (src.getChannel() == null) {
      assign(sources, idx.getNode(), idx.getPort(), topics);
    }

    cur.assign(src.getChannel());
  }

  @Getter @ToString
  public static class Meta {
    private final List<Node.Meta> nodes;
    private final List<Link> links;

    public Meta() {
      this.nodes = new LinkedList<>();
      this.links = new LinkedList<>();
    }
  }
}
