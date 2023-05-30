package jdk.tool;

import java.util.Arrays;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

/** A runner of tools. */
public interface ToolRunner {
  ToolFinder finder();

  default void runCompactCommand(String command) {
    var lines =
        command
            .lines()
            .map(String::trim)
            .filter(line -> !line.startsWith("#"))
            .map(line -> line.split("\\s"))
            .flatMap(Stream::of)
            .toArray(String[]::new);
    if (lines.length == 0) throw new RuntimeException("No tool in compact command: " + command);
    var tool = lines[0];
    var args = Arrays.copyOfRange(lines, 1, lines.length);
    run(tool, args);
  }

  default void run(ToolCall call) {
    run(call.tool(), call.arguments().toArray(String[]::new));
  }

  default void run(String tool, String... args) {
    run(finder().findToolOrElseThrow(tool), args);
  }

  default void run(ToolProvider tool, String... args) {
    run(Tool.of(tool), args);
  }

  void run(Tool tool, String... args);
}
