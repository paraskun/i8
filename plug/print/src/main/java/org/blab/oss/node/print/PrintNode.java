package org.blab.oss.node.print;

import org.blab.oss.i8.Node;
import org.blab.oss.i8.util.Empty;

import java.io.Serializable;

public class PrintNode extends Node<Empty, Empty, PrintNode.Log> {
    public PrintNode(Node.Meta meta) {
        super(meta);
    }

    @Override
    public Class<Empty> getConfigurationClass() {
        return Empty.class;
    }

    @Override
    public Class<Empty> getStdInClass() {
        return Empty.class;
    }

    @Override
    public Class<Log> getStdOutClass() {
      return Log.class;
    }

    @Override
    protected void build(Empty e) {
        port(0).asSource()
                .map(r -> new Log(r.toString()))
                .returns(Log.class)
                .sinkTo(stdOut());
    }

    public static class Log implements Serializable {
        String text;

        public Log() {
        }

        public Log(String text) {
            this.text = text;
        }
    }
}
