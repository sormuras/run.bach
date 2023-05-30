package run.bach;

import java.util.Arrays;
import jdk.tool.Tool;
import jdk.tool.ToolFinder;
import jdk.tool.ToolRunner;
import jdk.tool.Toolbox;
import jdk.tool.Tools;

public record Bach(Toolbox toolbox) implements ToolRunner {

  private static final InheritableThreadLocal<Bach> CURRENT =
      new InheritableThreadLocal<>() {
        @Override
        protected Bach initialValue() {
          return new Bach(Toolbox.ofSystemPrinter());
        }
      };

  public static Bach currentBach() {
    return CURRENT.get();
  }

  private static void currentBach(Bach bach) {
    CURRENT.set(bach);
  }

  private static final System.Logger logger = System.getLogger(Bach.class.getName());

  int run(String... args) {
    logger.log(System.Logger.Level.DEBUG, "Running Bach...");

    currentBach(this);

    var out = toolbox.printer().out();
    var err = toolbox.printer().err();

    if (args.length == 0) {
      out.println("Usage: Bach [<options>] <tool> [<args>...]");
      out.println();
      out.println("Available tools include:");
      out.println(Tools.toString(toolbox));
      return 0;
    }

    var found = toolbox.findTool(args[0]);
    if (found.isEmpty()) {
      err.println("Tool not found: " + args[0]);
      err.println();
      err.println("Available tools include:");
      err.println(Tools.toString(toolbox));
      return 1;
    }
    var tool = found.get();
    run(tool, Arrays.copyOfRange(args, 1, args.length));
    return 0;
  }

  @Override
  public ToolFinder finder() {
    return toolbox;
  }

  public void run(Tool tool, String... args) {
    currentBach(this);
    toolbox.run(tool, args);
  }
}
