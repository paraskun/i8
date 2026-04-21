package org.blab.oss.i8.boot;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.blab.oss.i8.Node;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class Job {
  private final String id;

  @Builder
  @Getter @ToString
  public static class Description {
    private List<Node.Meta> nodes;
  }
}
