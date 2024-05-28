/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach;

import java.net.URI;
import java.util.List;

/** Connects zero or more module names to their locations, usually uniform resource identifiers. */
@FunctionalInterface
public interface ModuleLocator {
  /**
   * Finds a location for a module of a given name.
   *
   * @param name The name of the module to locate
   * @return A location a module with the given name
   */
  Location locate(String name);

  /** A nominal reference to the location of a module. */
  sealed interface Location extends ModuleLocator {
    static Location of(String name, String location) {
      return of(name, URI.create(location));
    }

    static Location of(String name, URI location) {
      return new Uniform(name, location);
    }

    static Location unknown(String name) {
      return new Unknown(name);
    }

    record Unknown(String name) implements Location {
      @Override
      public Location locate(String name) {
        return this;
      }
    }

    record Uniform(String name, URI uri) implements Location {
      @Override
      public Location locate(String name) {
        return this.name.equals(name) ? this : new Unknown(name);
      }
    }
  }

  static ModuleLocator of(String name, String location) {
    return Location.of(name, location);
  }

  static ModuleLocator compose(ModuleLocator... lookups) {
    return new CompositeLocator(List.of(lookups));
  }

  record CompositeLocator(List<ModuleLocator> locators) implements ModuleLocator {
    public CompositeLocator {
      locators = List.copyOf(locators);
    }

    @Override
    public Location locate(String name) {
      for (var locator : locators) {
        var location = locator.locate(name);
        if (!(location instanceof Location.Unknown)) return location;
      }
      return new Location.Unknown(name);
    }
  }
}
