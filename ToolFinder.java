/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach;

import java.lang.module.ModuleFinder;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import run.bach.internal.ModulesSupport;

/**
 * A finder of tools.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * ToolFinder.compose(
 *     ToolFinder.of("jar", "javac", "javadoc"),
 *     ToolFinder.of("java", "jfr")
 * )
 * }</pre>
 */
@FunctionalInterface
public interface ToolFinder {
  /** {@return a list of tool instances of this finder, possibly empty} */
  List<Tool> tools();

  /**
   * {@return an instance of a tool for the given tool-identifying name}
   *
   * @param name the name of the tool to lookup
   */
  default Optional<Tool> find(String name) {
    return tools().stream().filter(tool -> tool.identifier().matches(name)).findFirst();
  }

  /**
   * {@return an instance of a tool for the given tool-identifying name}
   *
   * @param name the name of the tool to lookup
   * @throws ToolNotFoundException when a tool could not be found the given name
   */
  default Tool get(String name) {
    var tool = find(name);
    if (tool.isPresent()) return tool.get();
    throw new ToolNotFoundException("Tool not found for name: " + name);
  }

  /**
   * {@return a tool finder composed all tools specified by their names}
   *
   * @param tools the names of the tools to be looked-up
   * @throws ToolNotFoundException if any tool could not be found
   */
  static ToolFinder of(String... tools) {
    return of(Stream.of(tools).map(Tool::of).toArray(Tool[]::new));
  }

  /**
   * {@return a tool finder composed of a sequence of zero or more tools}
   *
   * @param tools the array of tools
   */
  static ToolFinder of(Tool... tools) {
    return new DefaultFinder(List.of(tools));
  }

  static ToolFinder of(ModuleFinder finder) {
    var layer = ModulesSupport.buildModuleLayer(finder);
    return of(layer);
  }

  static ToolFinder of(ModuleLayer layer) {
    var tools =
        ServiceLoader.load(layer, ToolProvider.class).stream()
            .filter(service -> service.type().getModule().getLayer() == layer)
            .map(ServiceLoader.Provider::get)
            .map(Tool::of)
            .toList();
    return new DefaultFinder(tools);
  }

  static ToolInstaller.Finder ofInstaller() {
    return ToolInstaller.finder(ToolInstaller.Mode.INSTALL_ON_DEMAND);
  }

  static ToolInstaller.Finder ofInstaller(ToolInstaller.Mode mode) {
    return ToolInstaller.finder(mode);
  }

  static ToolFinder ofSystem() {
    return new SystemFinder();
  }

  /**
   * {@return a tool finder that is composed of a sequence of zero or more tool finders}
   *
   * @param finders the array of tool finders
   */
  static ToolFinder compose(ToolFinder... finders) {
    return new CompositeFinder(List.of(finders));
  }

  record CompositeFinder(List<ToolFinder> finders) implements ToolFinder {

    public CompositeFinder {
      finders = List.copyOf(finders);
    }

    @Override
    public List<Tool> tools() {
      return finders.stream().flatMap(finder -> finder.tools().stream()).toList();
    }

    @Override
    public Optional<Tool> find(String name) {
      for (var finder : finders) {
        var tool = finder.find(name);
        if (tool.isPresent()) return tool;
      }
      return Optional.empty();
    }
  }

  record DefaultFinder(List<Tool> tools) implements ToolFinder {
    public DefaultFinder {
      tools = List.copyOf(tools);
    }
  }

  record SystemFinder() implements ToolFinder {
    @Override
    public List<Tool> tools() {
      // TODO Load tool providers using the context class loader.
      // TODO List tool programs of the current JDK's bin folder.
      return List.of();
    }

    @Override
    public Optional<Tool> find(String name) {
      try {
        return Optional.of(Tool.of(name));
      } catch (ToolNotFoundException exception) {
        return Optional.empty();
      }
    }
  }
}
