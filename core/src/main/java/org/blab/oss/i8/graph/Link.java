package org.blab.oss.i8.graph;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter @ToString
@RequiredArgsConstructor
public class Link {
  private final End source;
  private final End target;

  @Getter @ToString
  @RequiredArgsConstructor
  public static class End {
    private final int node;
    private final int port;
  }
}
