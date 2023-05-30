package jdk.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.TreeMap;

/** This class consists exclusively of static methods that operate on tools and related types. */
public final class Tools {
  public static String computeDefaultNamespace(Object object) {
    var type = object instanceof Class<?> instance ? instance : object.getClass();
    var module = type.getModule();
    return module.isNamed() ? module.getName() : type.getPackageName();
  }

  public static Optional<String> computeDefaultVersion(Object object) {
    var type = object instanceof Class<?> instance ? instance : object.getClass();
    var pkg = type.getPackage();
    var module = type.getModule();
    var descriptor = module.getDescriptor();
    var version =
        module.isNamed()
            ? descriptor.version().map(Object::toString).orElseGet(pkg::getImplementationVersion)
            : pkg.getImplementationVersion();
    return Optional.ofNullable(version);
  }

  public static String toString(ToolFinder finder) {
    return toString(finder.tools());
  }

  public static String toString(List<Tool> tools) {
    if (tools.isEmpty()) return "No tools";
    if (tools.size() == 1) {
      var tool = tools.get(0);
      return tool.name() + " = " + tool.toNamespaceAndNameAndVersion();
    }
    var map = new TreeMap<String, List<Tool>>();
    var max = 0;
    for (var tool : tools) {
      var name = tool.name();
      map.computeIfAbsent(name, __ -> new ArrayList<>()).add(tool);
      if (name.length() > max) max = name.length();
    }
    var lines = new StringJoiner("\n");
    var format = "%" + max + "s = %s";
    for (var entry : map.entrySet()) {
      var name = entry.getKey();
      var value = entry.getValue();
      var tool = value.get(0);
      var line = format.formatted(name, tool.toNamespaceAndNameAndVersion());
      if (value.size() == 1) {
        lines.add(line);
        continue;
      }
      var more = value.stream().skip(1);
      lines.add(line + " " + more.map(Tool::toNamespaceAndNameAndVersion).toList());
    }
    lines.add("   %d tools%n".formatted(tools.size()));
    return lines.toString();
  }

  private Tools() {}
}
