package org.blab.oss.i8.flink;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.blab.oss.i8.boot.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;

@Component
public class FlinkClient {
  @Autowired
  private Gson gson;

  private final RestClient client;
  private final String bootJar;

  public FlinkClient(
          @Value("${flink.rest.server}") String server,
          @Value("${root.path}") String rootPath
  ) {
    client = RestClient.builder()
            .baseUrl(String.format("http://%s", server))
            .build();

    bootJar = uploadJar(String.format("%s/boot.jar", rootPath));
  }

  private String uploadJar(String path) {
    var body = new LinkedMultiValueMap<String, Resource>();

    body.add("jarfile", new FileSystemResource(path));

    var token = Objects.requireNonNull(client.post()
            .uri("/jars/upload")
            .header("Content-Type", "multipart/form-data")
            .body(body)
            .retrieve()
            .toEntity(JarUploadResponseBody.class).getBody()).filename.split("/");

    return token[token.length - 1];
  }

  public Job submit(Job.Description description) {
    var response = client.post()
            .uri("/jars/{jar}/run", bootJar)
            .body(new JarRunRequestBody(List.of("--description",
                    gson.toJson(description))))
            .retrieve()
            .toEntity(JarRunResponseBody.class);

    if (response.getStatusCode() != HttpStatusCode.valueOf(200)) {
      throw new RuntimeException();
    }

    return new Job(Objects.requireNonNull(response.getBody()).getJobid());
  }

  public void terminate(Job job) {
    var response = client.patch()
            .uri("/jobs/{job}", job.getId())
            .retrieve()
            .toBodilessEntity();

    if (response.getStatusCode() != HttpStatusCode.valueOf(202)) {
      throw new RuntimeException();
    }
  }

  @Getter
  @Setter
  static class JarUploadResponseBody {
    String filename;
    String status;
  }

  @Getter
  @Setter
  @AllArgsConstructor
  static class JarRunRequestBody {
    List<String> programArgsList;
  }

  @Getter
  @Setter
  static class JarRunResponseBody {
    String jobid;
  }
}
