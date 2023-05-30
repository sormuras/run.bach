package jdk.tool;

import java.util.List;
import java.util.Optional;

/** A finder of tool instances. */
@FunctionalInterface
public interface ToolFinder {
  List<Tool> tools();

  default Optional<Tool> findTool(String string) {
    return tools().stream().filter(tool -> tool.matches(string)).findFirst();
  }

  default Tool findToolOrElseThrow(String string) {
    var found = findTool(string);
    if (found.isPresent()) return found.get();
    throw new ToolNotFoundException("Tool not found: " + string);
  }
}
