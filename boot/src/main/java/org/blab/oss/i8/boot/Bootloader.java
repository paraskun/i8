package org.blab.oss.i8.boot;

import com.google.gson.Gson;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.ParameterTool;
import org.blab.oss.i8.Node;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;

public class Bootloader {
  public static void main(String[] args) throws Exception {
    var parameters = ParameterTool.fromArgs(args);
    var description = new Gson().fromJson(parameters.getRequired("description"), Job.Description.class);
    var configuration = new Configuration();

    var nodePath = System.getenv("NODE_PATH");
    var plugPath = new StringBuilder();

    for (int i = 0; i < description.getNodes().size(); ++i) {
      var node = description.getNodes().get(i);

      if (i != 0) {
        plugPath.append(';');
      }

      plugPath.append(String.format("file://%s/%s/plugin.jar", nodePath, node.getType()));
    }

    configuration.setString("pipeline.classpaths", plugPath.toString());

    var loaders = new LinkedList<URLClassLoader>();

    try (var executionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment(configuration)) {
      var environment = new Environment(executionEnvironment);

      for (var node : description.getNodes()) {
        var loader = new URLClassLoader(new URL[]{
                new File(String.format("%s/%s/plugin.jar", nodePath, node.getType()))
                        .toURI()
                        .toURL()
        }, Thread.currentThread().getContextClassLoader());

        loaders.add(loader);
        Node.create(loader, node).build(environment);
      }

      executionEnvironment.execute();
    } finally {
      loaders.forEach(l -> {
        try {
          l.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
    }
  }
}
