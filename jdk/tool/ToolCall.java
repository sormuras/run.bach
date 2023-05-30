package jdk.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Stream;

/**
 * Represents a command from the command-line.
 *
 * <p>For example: a command {@code javac --version} can be composed via:
 *
 * <ul>
 *   <li>{@code ToolCall.ofCommandLine("javac --version")}
 *   <li>{@code ToolCall.ofCommand(List.of("javac", "--version")}
 *   <li>{@code ToolCall.of("javac").with("--version")}
 *   <li>{@code ToolCall.of("javac", "--version")}
 * </ul>
 *
 * @param tool the name of the tool to run
 * @param arguments the arguments to pass to the tool being run
 */
public record ToolCall(String tool, List<String> arguments) {
  public static ToolCall of(String tool) {
    return new ToolCall(tool);
  }

  public static ToolCall of(String tool, Object... args) {
    if (args.length == 0) return new ToolCall(tool);
    if (args.length == 1) return new ToolCall(tool, trim(args[0]));
    if (args.length == 2) return new ToolCall(tool, trim(args[0]), trim(args[1]));
    return new ToolCall(tool).with(Stream.of(args));
  }

  // command = ["tool-name", "tool-args", ...]
  public static ToolCall ofCommand(List<String> command) {
    var size = command.size();
    if (size == 0) throw new IllegalArgumentException("Empty command");
    var tool = command.get(0);
    if (size == 1) return new ToolCall(tool);
    if (size == 2) return new ToolCall(tool, trim(command.get(1)));
    if (size == 3) return new ToolCall(tool, trim(command.get(1)), trim(command.get(2)));
    return new ToolCall(tool).with(command.stream().skip(1).map(ToolCall::trim));
  }

  // line = "tool-name [tool-args...]"
  public static ToolCall ofCommandLine(String line) {
    return ToolCall.ofCommand(List.of(trim(line).split("\\s+")));
  }

  private static String trim(Object object) {
    return object.toString().trim();
  }

  private ToolCall(String tool, String... args) {
    this(tool, List.of(args));
  }

  public String[] toArray() {
    return arguments.toArray(String[]::new);
  }

  public String toCommandLine() {
    return toCommandLine(" ");
  }

  public String toCommandLine(String delimiter) {
    if (arguments.isEmpty()) return tool;
    if (arguments.size() == 1) return tool + delimiter + arguments.get(0);
    var joiner = new StringJoiner(delimiter).add(tool);
    arguments.forEach(joiner::add);
    return joiner.toString();
  }

  public ToolCall with(Stream<?> objects) {
    var strings = objects.map(ToolCall::trim);
    return new ToolCall(tool, Stream.concat(arguments.stream(), strings).toList());
  }

  public ToolCall with(String[] arguments) {
    return with(Stream.of(arguments));
  }

  public ToolCall with(Object argument) {
    return with(Stream.of(argument));
  }

  public ToolCall with(String key, Object value, Object... values) {
    var call = with(Stream.of(key, value));
    return values.length == 0 ? call : call.with(Stream.of(values));
  }

  public ToolCall withFindFiles(String glob) {
    return withFindFiles(Path.of(""), glob);
  }

  public ToolCall withFindFiles(Path start, String glob) {
    return withFindFiles(start, "glob", glob);
  }

  public ToolCall withFindFiles(Path start, String syntax, String pattern) {
    var syntaxAndPattern = syntax + ':' + pattern;
    var matcher = start.getFileSystem().getPathMatcher(syntaxAndPattern);
    return withFindFiles(start, Integer.MAX_VALUE, matcher);
  }

  public ToolCall withFindFiles(Path start, int maxDepth, PathMatcher matcher) {
    try (var files = Files.find(start, maxDepth, (p, a) -> matcher.matches(p))) {
      return with(files);
    } catch (Exception exception) {
      throw new RuntimeException("Find files failed in: " + start, exception);
    }
  }

  public ToolCall withTweak(Tweak tweak) {
    return tweak.tweak(this);
  }

  public ToolCall withTweak(int position, Tweak tweak) {
    var call = new ToolCall(tool, List.of()).with(arguments.stream().limit(position));
    return tweak.tweak(call).with(arguments.stream().skip(position));
  }

  public ToolCall withTweaks(Iterable<Tweak> tweaks) {
    var tweaked = this;
    for (var tweak : tweaks) tweaked = tweak.tweak(tweaked);
    return tweaked;
  }

  /** Represents a unary operation on a tool call producing a new tool call with other arguments. */
  @FunctionalInterface
  public interface Tweak {
    ToolCall tweak(ToolCall call);
  }
}
