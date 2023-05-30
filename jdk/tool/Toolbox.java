package jdk.tool;

import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

/** A configurable tool-finding runner of tools. */
public record Toolbox(List<Tool> tools, Printer printer) implements ToolFinder, ToolRunner {

  private static final System.Logger logger = System.getLogger(Toolbox.class.getName());

  public record Printer(PrintWriter out, PrintWriter err) {}

  public static Toolbox ofSystemPrinter(Tool... tools) {
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    return Toolbox.of(out, err, tools);
  }

  public static Toolbox of(PrintWriter out, PrintWriter err, Tool... tools) {
    var printer = new Printer(out, err);
    return new Toolbox(List.of(tools), printer);
  }

  public Toolbox {
    Objects.requireNonNull(printer);
    Objects.requireNonNull(tools);
    tools = List.copyOf(tools);
  }

  private Toolbox with(List<Tool> tools) {
    return new Toolbox(tools, printer);
  }

  private static void log(Tool tool) {
    logger.log(System.Logger.Level.DEBUG, " + {0}", tool.toNamespaceAndNameAndVersion());
  }

  public Toolbox withTools(List<Tool> tools) {
    return withTools(tools.stream());
  }

  public Toolbox withTools(Stream<Tool> stream) {
    return with(Stream.concat(tools.stream(), stream.peek(Toolbox::log)).toList());
  }

  public Toolbox withTool(ToolProvider tool, ToolProvider... more) {
    return withTools(Stream.concat(Stream.of(tool), Stream.of(more)).map(Tool::of));
  }

  public Toolbox withTool(Tool tool, Tool... more) {
    return withTools(Stream.concat(Stream.of(tool), Stream.of(more)));
  }

  public Toolbox withTool(String name, String... more) {
    return withTools(Stream.concat(Stream.of(name), Stream.of(more)).map(Tool::of));
  }

  public Toolbox withToolProviderServices() {
    var tools =
        ServiceLoader.load(ToolProvider.class, getClass().getClassLoader()).stream()
            .map(ServiceLoader.Provider::get)
            .map(Tool::of);
    return withTools(tools);
  }

  @Override
  public ToolFinder finder() {
    return this;
  }

  @Override
  public void run(Tool tool, String... args) {
    logger.log(System.Logger.Level.INFO, "{0} {1}", tool.name(), String.join(" ", args));
    try (var event = ToolRunEvent.begin(tool, args)) {
      event.code = tool.provider().run(printer.out, printer.err, args);
    }
  }
}
