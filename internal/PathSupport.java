/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.internal;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import run.bach.info.PackageUrl;

public interface PathSupport {
  static String checksum(Path file, String algorithm) {
    if (Files.notExists(file)) throw new RuntimeException("File not found: " + file);
    try {
      if ("size".equalsIgnoreCase(algorithm)) return Long.toString(Files.size(file));
      var md = MessageDigest.getInstance(algorithm);
      try (var source = new BufferedInputStream(new FileInputStream(file.toFile()));
          var target = new DigestOutputStream(OutputStream.nullOutputStream(), md)) {
        source.transferTo(target);
      }
      var format = "%0" + (md.getDigestLength() * 2) + "x";
      return String.format(format, new BigInteger(1, md.digest()));
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  static boolean isJarFile(Path path) {
    return name(path, "").endsWith(".jar") && Files.isRegularFile(path);
  }

  static boolean isJavaFile(Path path) {
    return name(path, "").endsWith(".java") && Files.isRegularFile(path);
  }

  static boolean isPropertiesFile(Path path) {
    return name(path, "").endsWith(".properties") && Files.isRegularFile(path);
  }

  static List<Path> list(Path directory, DirectoryStream.Filter<? super Path> filter) {
    if (Files.notExists(directory)) return List.of();
    var paths = new TreeSet<>(Comparator.comparing(Path::toString));
    try (var stream = Files.newDirectoryStream(directory, filter)) {
      stream.forEach(paths::add);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
    return List.copyOf(paths);
  }

  static String name(Path path, String defaultName) {
    var normalized = path.normalize();
    var candidate = normalized.toString().isEmpty() ? normalized.toAbsolutePath() : normalized;
    var name = candidate.getFileName();
    return name != null ? name.toString() : defaultName;
  }

  static void copy(Path target, URI source) {
    if (!Files.exists(target)) {
      if (source.getScheme().equals("pkg")) {
        source = PackageUrl.parse(source).toUri();
      }
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
  }
}
