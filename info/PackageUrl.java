/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.info;

import java.net.URI;

/**
 * A package (mostly) universal URL.
 *
 * @see <a href="https://github.com/package-url/purl-spec">Specification</a>
 */
public sealed interface PackageUrl {

  URI toUri();

  static PackageUrl parse(URI uri) {
    if (!uri.getScheme().equals("pkg")) throw new IllegalArgumentException(uri.toString());
    return parse(uri.toString());
  }

  static PackageUrl parse(String string) {
    if (string.startsWith("pkg:maven/")) return MavenPurl.parse(string);
    throw new UnsupportedOperationException(string);
  }

  record MavenPurl(MavenCoordinate coordinate) implements PackageUrl {
    public static MavenPurl parse(String string) {
      var groupAndFile = string.split("/");
      assert groupAndFile[0].equals("pkg:maven");
      var group = groupAndFile[1];
      var artifactAndVersion = groupAndFile[2].split("@");
      var artifact = artifactAndVersion[0];
      var version = artifactAndVersion[1];
      var coordinate = MavenCoordinate.ofCentral(group, artifact, version);
      return new MavenPurl(coordinate);
    }

    @Override
    public URI toUri() {
      return coordinate.toUri();
    }
  }
}
