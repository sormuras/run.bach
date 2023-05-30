package jdk.tool;

import java.util.Objects;
import java.util.Optional;
import java.util.spi.ToolProvider;

/** A container object for a Java tool. */
public record Tool(String namespace, String name, Optional<String> version, ToolProvider provider) {
  private static final System.Logger logger = System.getLogger(Tool.class.getName());

  public static Tool of(String namespace, String name, String version, ToolProvider provider) {
    return new Tool(namespace, name, Optional.ofNullable(version), provider);
  }

  public static Tool of(ToolProvider provider) {
    var namespace = Tools.computeDefaultNamespace(provider);
    var name = provider.name();
    var version = Tools.computeDefaultVersion(provider);
    return new Tool(namespace, name, version, provider);
  }

  public static Tool of(String name) {
    return find(name).orElseThrow(() -> new ToolNotFoundException("Tool not found: " + name));
  }

  public static Optional<Tool> find(String name) {
    logger.log(System.Logger.Level.TRACE, "Find tool by name: " + name);
    var provider = ToolProvider.findFirst(name);
    if (provider.isPresent()) return Optional.of(Tool.of(provider.get()));
    var program = ToolProgram.findJavaDevelopmentKitTool(name);
    if (program.isPresent()) {
      var version = Runtime.version().toString();
      return Optional.of(Tool.of("java.home/bin", name, version, program.get()));
    }
    return Optional.empty();
  }

  public Tool {
    Objects.requireNonNull(namespace);
    Objects.requireNonNull(name);
    Objects.requireNonNull(version);
    Objects.requireNonNull(provider);
  }

  public boolean matches(String string) {
    if (name.equals(string)) return true; // "javac"
    if (toNamespaceAndName().equals(string)) return true; // "jdk.compiler/javac"
    if (version.isPresent()) {
      if (toNameAndVersion().equals(string)) return true; // "javac@99"
      return toNamespaceAndNameAndVersion().equals(string); // "jdk.compiler/javac@99"
    }
    return false;
  }

  public String toNameAndVersion() {
    return version.map(version -> name + '@' + version).orElse(name);
  }

  public String toNamespaceAndName() {
    return namespace + '/' + name;
  }

  public String toNamespaceAndNameAndVersion() {
    return namespace + '/' + toNameAndVersion();
  }
}
