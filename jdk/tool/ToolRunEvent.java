package jdk.tool;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Category({"Java Development Kit", "Tool"})
@StackTrace(false)
@Label("Tool Run")
@Name("jdk.tool.ToolRun")
@Description("Command-line configuration, runtime output, and exit code of a tool run")
final class ToolRunEvent extends Event implements AutoCloseable {
  static ToolRunEvent begin(Tool tool, String... args) {
    var event = new ToolRunEvent();
    if (event.isEnabled()) {
      event.tool = tool.toNamespaceAndNameAndVersion();
      event.name = tool.name();
      event.args = String.join(" ", args);
      event.begin();
    }
    return event;
  }

  @Override
  public void close() {
    if (shouldCommit()) commit();
  }

  String tool;

  String name;

  String args;

  int code;
}
