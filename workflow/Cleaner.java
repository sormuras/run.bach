/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public interface Cleaner extends Action {
  default void clean() {
    say("Cleaning ...");
    var paths = cleanerUsesPathsForDeletion().stream().filter(Files::exists).toList();
    if (paths.isEmpty()) return;
    for (var path : paths) {
      var kind =
          Files.isRegularFile(path) ? "file" : Files.isDirectory(path) ? "directory" : "path";
      log("Deleting %s %s ...".formatted(kind, path.toUri()));
      cleanerDeleteRecursively(path);
    }
  }

  default List<Path> cleanerUsesPathsForDeletion() {
    return List.of(workflow().folders().out());
  }

  default void cleanerDeleteRecursively(Path file) {
    if (!Files.exists(file)) return;
    try {
      try {
        log("Delete " + file.toUri());
        Files.deleteIfExists(file); // delete a regular file or an empty directory
      } catch (DirectoryNotEmptyException exception) {
        try (var stream = Files.walk(file)) {
          var paths = stream.sorted(Comparator.reverseOrder()).toList();
          for (var path : paths) {
            workflow().runner().log(Level.TRACE, "Delete " + path.toUri());
            Files.deleteIfExists(path);
          }
        }
      }
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  default void cleanerPrune(Path directory, DirectoryStream.Filter<? super Path> filter) {
    try (var stream = Files.newDirectoryStream(directory, filter)) {
      for (var file : stream) cleanerDeleteRecursively(file);
    } catch (IOException exception) {
      throw new UncheckedIOException("Pruning failed: " + directory, exception);
    }
  }
}
