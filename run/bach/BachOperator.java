package run.bach;

import java.util.spi.ToolProvider;
import jdk.tool.Tool;
import jdk.tool.ToolFinder;
import jdk.tool.ToolRunner;

public interface BachOperator extends ToolProvider, ToolRunner {
  default Bach bach() {
    return Bach.currentBach();
  }

  @Override
  default ToolFinder finder() {
    return bach().toolbox();
  }

  @Override
  default void run(Tool tool, String... args) {
    bach().toolbox().run(tool, args);
  }
}
