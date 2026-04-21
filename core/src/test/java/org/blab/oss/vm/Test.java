package org.blab.oss.vm;

import org.apache.avro.Schema;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.blab.oss.i8.Node;
import org.blab.oss.i8.Port;
import org.blab.oss.node.print.PrintNode;
import org.blab.oss.node.write.WriteNode;

import java.util.List;

public class Test {
  public static void main(String[] args) {
    try (var environment = StreamExecutionEnvironment.createLocalEnvironment()) {
      new WriteNode(org.blab.oss.i8.Node.Description.builder()
              .id(0)
              .ports(List.of(new org.blab.oss.i8.Port.Description("i8.port.0.0",
                      new Schema.Parser().parse(Thread.currentThread()
                                      .getContextClassLoader()
                                      .getResourceAsStream("schema/DoubleValue.avsc"))
                              .toString()
              ))).build()).build(new Environment(environment));

      new PrintNode(Node.Description.builder()
              .id(1)
              .ports(List.of(new Port.Description("i8.port.0.0",
                      new Schema.Parser().parse(Thread.currentThread()
                                      .getContextClassLoader()
                                      .getResourceAsStream("schema/DoubleValue.avsc"))
                              .toString()
              ))).build()).build(new Environment(environment));

      environment.execute();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
