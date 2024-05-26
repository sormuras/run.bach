/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import jdk.jfr.Category;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;
import run.bach.internal.ModulesSupport;

public interface ModuleResolver {
  void resolveModule(String name);

  void resolveMissingModules();

  static ModuleResolver ofSingleDirectory(Path directory, String properties) {
    return ofSingleDirectory(directory, ModuleFinders.ofProperties(properties));
  }

  static ModuleResolver ofSingleDirectory(Path directory, ModuleFinder finder) {
    return new CanonicalResolver(directory, finder);
  }

  record CanonicalResolver(Path directory, ModuleFinder finder) implements ModuleResolver {
    @Override
    public void resolveModule(String name) {
      var module = ModuleFinder.of(directory).find(name);
      if (module.isPresent()) {
        Event.AlreadyResolved.commit(module.get());
        return;
      }
      var found = finder.find(name);
      if (found.isEmpty()) {
        throw new IllegalStateException("Module not locatable: " + name);
      }
      var source = found.get().location().orElseThrow();
      var target = directory.resolve(name + ".jar");
      var event = Event.ResolveModule.begin(name, source, target);
      ModuleResolver.copy(target, source);
      event.commit();
    }

    @Override
    public void resolveMissingModules() {
      var resolved = new TreeSet<String>();
      var difference = new TreeSet<String>();
      var event = new Event.ResolveModules();
      event.begin();
      while (true) {
        var finders = List.of(ModuleFinder.of(directory)); // recreate in every loop
        var missing = ModulesSupport.listMissingNames(finders, Set.of());
        if (missing.isEmpty()) break;
        difference.retainAll(missing);
        if (!difference.isEmpty()) throw new IllegalStateException("Still missing?! " + difference);
        difference.addAll(missing);
        missing.forEach(this::resolveModule);
        resolved.addAll(missing);
      }
      event.commit(resolved);
    }

    @Category("Bach")
    @StackTrace(false)
    abstract static sealed class Event extends jdk.jfr.Event {
      @Name("run.bach.ModuleResolverAlreadyResolved")
      static final class AlreadyResolved extends Event {
        String module;
        String target;

        static void commit(ModuleReference reference) {
          var event = new AlreadyResolved();
          if (event.shouldCommit()) {
            event.module = reference.descriptor().name();
            event.target = reference.location().map(URI::toString).orElse("?");
            event.commit();
          }
        }
      }

      @Name("run.bach.ModuleResolverResolvedModule")
      static final class ResolveModule extends Event {
        String name;
        String source;
        String target;

        static ResolveModule begin(String name, URI source, Path target) {
          var event = new ResolveModule();
          event.begin();
          event.name = name;
          event.source = source.toString();
          event.target = target.toString();
          return event;
        }
      }

      @Name("run.bach.ModuleResolverResolvedModules")
      static final class ResolveModules extends Event {
        int count;
        String names;
        void commit(Collection<String> modules) {
          count = modules.size();
          names = String.join(",", modules);
          commit();
        }
      }
    }
  }

  private static void copy(Path target, URI source) {
    if (!Files.exists(target)) {
      try (var stream =
          source.getScheme().startsWith("http")
              ? source.toURL().openStream()
              : Files.newInputStream(Path.of(source))) {
        var parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException exception) {
        throw new UncheckedIOException(exception);
      }
    }
    // TODO Verify target bits.
  }
}
