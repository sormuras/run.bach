package jdk.tool;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

/** A tool provider implementation running operating system programs. */
public record ToolProgram(String name, List<String> command) implements ToolProvider {
  public static Optional<ToolProvider> findJavaDevelopmentKitTool(String name, Object... args) {
    var bin = Path.of(System.getProperty("java.home", ""), "bin");
    return findInFolder(name, bin, args);
  }

  public static Optional<ToolProvider> findInFolder(String name, Path folder, Object... args) {
    if (!Files.isDirectory(folder)) return Optional.empty();
    var win = System.getProperty("os.name", "").toLowerCase().startsWith("win");
    var file = name + (win && !name.endsWith(".exe") ? ".exe" : "");
    return findExecutable(name, folder.resolve(file), args);
  }

  public static Optional<ToolProvider> findExecutable(String name, Path file, Object... args) {
    if (!Files.isExecutable(file)) return Optional.empty();
    var command = new ArrayList<String>();
    command.add(file.toString());
    command.addAll(Stream.of(args).map(Object::toString).toList());
    return Optional.of(new ToolProgram(name, List.copyOf(command)));
  }

  public ToolProgram {
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(command, "command must not be null");
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... arguments) {
    var builder = new ProcessBuilder(new ArrayList<>(command));
    builder.command().addAll(List.of(arguments));
    try {
      var process = builder.start();
      new Thread(new LinePrinter(process.getInputStream(), out), name + "-out").start();
      new Thread(new LinePrinter(process.getErrorStream(), err), name + "-err").start();
      return process.waitFor();
    } catch (InterruptedException exception) {
      return -1;
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 1;
    }
  }

  record LinePrinter(InputStream stream, PrintWriter writer) implements Runnable {
    @Override
    public void run() {
      new BufferedReader(new InputStreamReader(stream)).lines().forEach(writer::println);
    }
  }
}
