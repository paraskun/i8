package org.blab.oss.i8;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import lombok.*;
import org.apache.avro.Schema;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.blab.oss.i8.codec.ReflectCodec;

import java.io.Serializable;
import java.util.*;

public abstract class Node<C, StdIn, StdOut> implements Serializable {
  @Getter
  private transient final Meta meta;
  private transient DataStream<StdIn> stdIn;
  private transient Sink<StdOut> stdOut;

  private final List<Port> ports;

  public static Node<?, ?, ?> create(ClassLoader classLoader, Meta meta) throws Exception {
    return (Node<?, ?, ?>) classLoader
            .loadClass(meta.getType())
            .getConstructor(Node.Meta.class)
            .newInstance(meta);
  }

  public Node(Meta meta) {
    this.meta = meta;
    this.ports = new ArrayList<>(meta.getPorts().size());
  }

  public void build() throws Exception {
    for (var d : meta.getPorts()) {
      ports.add(Port.builder()
              .schema(new Schema.Parser().parse(Thread.currentThread()
                      .getContextClassLoader()
                      .getResourceAsStream(String.format("schema/%s.avsc", d.getSchema()))))
              .build());
    }
  }

  public void build(GraphExecutionEnvironment environment) throws Exception {
    for (var d : meta.getPorts()) {
      ports.add(Port.builder()
              .environment(environment)
              .channel(d.getChannel())
              .schema(new Schema.Parser().parse(d.getSchema()))
              .build());
    }

    var si = String.format("i8.node.%d.stdin", meta.getId());
    var so = String.format("i8.node.%d.stdout", meta.getId());

    this.stdIn = environment.from(si, new ReflectCodec<>(getStdInClass()));
    this.stdOut = environment.into(so, new ReflectCodec<>(getStdOutClass()));

    build(new Gson().fromJson(meta.configuration, getConfigurationClass()));
  }

  public int numberOfPorts() {
    return ports.size();
  }

  public Port port(int i) {
    return ports.get(i);
  }

  public abstract Class<C> getConfigurationClass();

  public abstract Class<StdIn> getStdInClass();

  public abstract Class<StdOut> getStdOutClass();

  protected abstract void build(C configuration) throws Exception;

  protected DataStream<StdIn> stdIn() {
    return stdIn;
  }

  protected Sink<StdOut> stdOut() {
    return stdOut;
  }

  @Getter @ToString
  public static class Meta {
    private Integer id;
    private String type;
    private List<Port.Meta> ports;
    private JsonElement configuration;
    private Position position;

    @ToString
    @RequiredArgsConstructor
    public static class Position {
      private final int x;
      private final int y;
    }
  }
}
