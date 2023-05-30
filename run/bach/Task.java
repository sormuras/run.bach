package run.bach;

import java.io.PrintWriter;
import java.util.List;
import jdk.tool.ToolCall;

public record Task(String name, List<ToolCall> calls, boolean parallel) implements BachOperator {
  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var stream = parallel ? calls.parallelStream() : calls.stream();
    stream.forEach(this::run);
    return 0;
  }
}
