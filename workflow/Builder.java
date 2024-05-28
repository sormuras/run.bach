/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

public interface Builder extends Action, Cleaner, Compiler, Preparer, Tester {
  default void build() {
    var description = builderUsesProjectDescription();
    say("Building %s ...".formatted(description));

    if (builderDoesCleanAtTheBeginning()) {
      clean(); // delete output folders
    }
    prepare(); // resolve required and missing assets, aka "go offline"
    compile(); // translate module space source files into classes, modular JAR files, and an images
    test(); // execute programs using artifacts compiled artifacts

    say("Build of %s completed.".formatted(description));
  }

  default String builderUsesProjectDescription() {
    return workflow().structure().toNameAndVersion();
  }

  default boolean builderDoesCleanAtTheBeginning() {
    return false;
  }
}
