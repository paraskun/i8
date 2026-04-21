package org.blab.oss.i8.plugin;

import com.google.gson.Gson;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.blab.oss.i8.Node;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;

@Slf4j
@Getter
public class Plugin {
  private final Manifest manifest;
  private final URLClassLoader loader;
  private final Class<Node<?, ?, ?>> clazz;

  @SuppressWarnings("unchecked")
  public Plugin(String nodePath, Manifest manifest) {
    this.manifest = manifest;

    try {
      this.loader = new URLClassLoader(new URL[]{
              new File(String.format("%s/%s/plugin.jar", nodePath, manifest.getType()))
                      .toURI()
                      .toURL()
      }, Thread.currentThread().getContextClassLoader());

      this.clazz = (Class<Node<?, ?, ?>>) loader.loadClass(manifest.getType());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void close() {
    try {
      loader.close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Data
  public static class Manifest {
    private String type;
    private String name;
    private String dist;
    private String description;
    private Long modificationTime = 0L;
    private Boolean withStyle = false;

    public static Manifest read(File file) {
      try (var fis = new InputStreamReader(new FileInputStream(file))) {
        return new Gson().fromJson(fis, Manifest.class);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      Manifest manifest = (Manifest) o;
      return Objects.equals(type, manifest.type);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(type);
    }
  }
}
