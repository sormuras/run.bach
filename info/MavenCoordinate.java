/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.info;

import java.net.URI;
import java.util.StringJoiner;

/** Maven component representation. */
public record MavenCoordinate(
    String repository,
    String group,
    String artifact,
    String version,
    String classifier,
    String type) {
  public static final String CENTRAL_REPOSITORY = "repo.maven.apache.org/maven2";

  public static final String DEFAULT_CLASSIFIER = "", DEFAULT_TYPE = "jar";

  public static MavenCoordinate ofCentral(String group, String artifact, String version) {
    return new MavenCoordinate(
        CENTRAL_REPOSITORY, group, artifact, version, DEFAULT_CLASSIFIER, DEFAULT_TYPE);
  }

  public static MavenCoordinate ofCentral(
      String group, String artifact, String version, String classifier) {
    return new MavenCoordinate(
        CENTRAL_REPOSITORY, group, artifact, version, classifier, DEFAULT_TYPE);
  }

  public URI toUri() {
    var joiner = new StringJoiner("/").add("https://" + repository);
    joiner.add(group.replace('.', '/')).add(artifact).add(version);
    var file = artifact + '-' + (classifier.isBlank() ? version : version + '-' + classifier);
    return URI.create(joiner.add(file + '.' + type).toString());
  }
}
