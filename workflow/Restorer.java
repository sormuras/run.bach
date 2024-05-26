/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.nio.file.Path;
import java.util.Set;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingStream;
import run.bach.ModuleResolver;
import run.bach.internal.ModulesSupport;
import run.bach.workflow.Structure.DeclaredModules;
import run.bach.workflow.Structure.Space;

public interface Restorer extends Action {
  default void restore() {
    var lib = restorerUsesLibraryDirectory();
    var finder = workflow().structure().libraries();
    var resolver = ModuleResolver.ofSingleDirectory(lib, finder);
    try (var events = restorerUsesEventRecordingStream()) {
      say("Restoring required modules ...");
      restorerUsesModuleNames().forEach(resolver::resolveModule);
      events.stop();
    }
    try (var events = restorerUsesEventRecordingStream()) {
      say("Restoring missing modules recursively ...");
      resolver.resolveMissingModules();
      events.stop();
    }
  }

  default Path restorerUsesLibraryDirectory() {
    return workflow().folders().root("lib");
  }

  default Set<String> restorerUsesModuleNames() {
    return restorerFindMissingModuleNames();
  }

  default Set<String> restorerFindMissingModuleNames() {
    var spaces = workflow().structure().spaces();
    var finders =
        spaces.list().stream().map(Space::modules).map(DeclaredModules::toModuleFinder).toList();
    var missing = ModulesSupport.listMissingNames(finders, Set.of());
    if (missing.isEmpty()) return Set.of();
    return Set.copyOf(missing);
  }

  default RecordingStream restorerUsesEventRecordingStream() {
    var stream = new RecordingStream();
    stream.setOrdered(true);
    stream.setReuse(true);
    stream.onEvent("run.bach.ModuleResolverAlreadyResolved", this::restorerOnModulePresent);
    stream.onEvent("run.bach.ModuleResolverResolvedModule", this::restorerOnModuleResolved);
    stream.onEvent("run.bach.ModuleResolverResolvedModules", this::restorerOnModulesResolved);
    stream.startAsync();
    return stream;
  }

  default void restorerOnModulePresent(RecordedEvent event) {
    var module = event.getString("module");
    var target = event.getString("target");
    log("%s -> %s".formatted(module, target));
  }

  default void restorerOnModuleResolved(RecordedEvent event) {
    var name = event.getString("name");
    var source = event.getString("source");
    say("%s <- %s".formatted(name, source));
  }

  default void restorerOnModulesResolved(RecordedEvent event) {
    var count = event.getInt("count");
    var names = event.getString("names");
    var message =
        switch (count) {
          case 0 -> "No module was missing.";
          case 1 -> "Restored 1 missing module: %s".formatted(names);
          default -> "Restored %d missing modules: %s".formatted(count, names);
        };
    say(message);
  }
}
