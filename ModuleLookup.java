/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach;

import java.net.URI;

/** Connects zero or more module names to their uniform resource identifiers. */
@FunctionalInterface
public interface ModuleLookup {
  URI lookupModule(String name);
}
