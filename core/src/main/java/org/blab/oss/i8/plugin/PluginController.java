package org.blab.oss.i8.plugin;

import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/plug")
public class PluginController implements WebMvcConfigurer {
  @Value("${plug.path}") private String plugPath;
  @Value("${node.path}") private String nodePath;

  private final Map<String, Plugin.Manifest> manifests = new HashMap<>();
  private final Map<String, Plugin> plugins = new HashMap<>();

  public void fetch() {
    try {
      var files = new File(plugPath).listFiles();

      if (files == null) {
        return;
      }

      Arrays.stream(files)
              .filter(f -> f.getName().endsWith(".zip"))
              .forEach(f -> {
                var zip = new File(f.getAbsolutePath());
                var name = f.getName();
                var type = name.substring(0, name.length() - 4);
                var man = manifests.get(type);

                if (man == null || man.getModificationTime() < zip.lastModified()) {
                  var path = install(type, zip);
                  var dir = new File(path);
                  var sub = dir.listFiles();

                  if (sub == null) {
                    manifests.remove(type);
                    return;
                  }

                  var opt = Arrays.stream(sub)
                          .filter(file -> file.getName().equals("manifest.json"))
                          .findAny();

                  if (opt.isEmpty()) {
                    manifests.remove(type);
                    return;
                  }

                  man = Plugin.Manifest.read(opt.get());

                  man.setModificationTime(zip.lastModified());
                  man.setDist(String.format("plug/dist/%s", man.getType()));
                  man.setWithStyle(Arrays.stream(sub)
                          .anyMatch(file -> file.getName().equals("plugin.css")));

                  manifests.put(type, man);
                }
              });
    } catch (Exception e) {
      log.error("could not fetch plugins", e);
    }
  }

  private String install(String type, File zip) {
    var dir = String.format("%s/%s", nodePath, type);

    if (new File(dir).exists()) {
      if (!new File(dir).delete()) {
        log.warn("could not delete {}", dir);
      }
    }

    try (var zis = new ZipFile(zip)) {
      zis.extractAll(dir);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return dir;
  }

  public Plugin.Manifest read(String type) {
    var man = manifests.get(type);

    if (man == null) {
      fetch();
      man = manifests.get(type);
    }

    return man;
  }

  @GetMapping
  public Collection<Plugin.Manifest> readAll() {
    fetch();
    return manifests.values();
  }

  public Plugin load(Plugin.Manifest manifest) {
    if (!plugins.containsKey(manifest.getType())) {
      plugins.put(manifest.getType(), new Plugin(nodePath, manifest));
    }

    return plugins.get(manifest.getType());
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/plug/dist/**")
            .addResourceLocations(String.format("file://%s", nodePath))
            .setCachePeriod(0);
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/plug/dist/**")
            .allowedOrigins("*")
            .allowedHeaders("*")
            .allowedMethods("*");
  }
}
