package org.blab.oss.node.write;

import org.apache.flink.formats.avro.typeutils.GenericRecordAvroTypeInfo;
import org.blab.oss.i8.util.Empty;
import org.blab.oss.i8.Node;
import org.blab.oss.i8.codec.GenericCodec;

import java.io.Serializable;

public class WriteNode extends Node<Empty, WriteNode.Input, Empty> {
  public WriteNode(Node.Meta meta) {
    super(meta);
  }

  @Override
  public Class<Empty> getConfigurationClass() {
    return Empty.class;
  }

  @Override
  public Class<Input> getStdInClass() {
    return Input.class;
  }

  @Override
  public Class<Empty> getStdOutClass() {
    return Empty.class;
  }

  @Override
  protected void build(Empty e) {
    stdIn()
            .map(r -> new GenericCodec.RecordBuilder()
                    .with("value", r.value)
                    .build(port(0).getSchema()))
            .returns(new GenericRecordAvroTypeInfo(port(0).getSchema()))
            .sinkTo(port(0).asSink());
  }

  public static class Input implements Serializable {
    Double value;
  }
}
