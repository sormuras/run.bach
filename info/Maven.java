/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.info;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.StringJoiner;
import java.util.spi.ToolProvider;
import run.bach.ToolInstaller;
import run.bach.ToolProgram;

/**
 * Apache Maven is a software project management and comprehension tool.
 *
 * @see <a href="https://maven.apache.org">https://maven.apache.org</a>
 */
public interface Maven {
  String CENTRAL_REPOSITORY = "https://repo.maven.apache.org/maven2";

  /** Maven component representation. */
  record Component(
      String repository,
      String group,
      String artifact,
      String version,
      String classifier,
      String type) {
    public static final String DEFAULT_CLASSIFIER = "", DEFAULT_TYPE = "jar";

    public static Component central(String group, String artifact, String version) {
      return new Component(
          CENTRAL_REPOSITORY, group, artifact, version, DEFAULT_CLASSIFIER, DEFAULT_TYPE);
    }

    public static Component central(
        String group, String artifact, String version, String classifier) {
      return new Component(CENTRAL_REPOSITORY, group, artifact, version, classifier, DEFAULT_TYPE);
    }

    public URI toUri() {
      var joiner = new StringJoiner("/").add(repository);
      joiner.add(group.replace('.', '/')).add(artifact).add(version);
      var file = artifact + '-' + (classifier.isBlank() ? version : version + '-' + classifier);
      return URI.create(joiner.add(file + '.' + type).toString());
    }
  }

  /**
   * Apache Maven tool installer.
   *
   * @see <a href="https://maven.apache.org">https://maven.apache.org</a>
   */
  record Installer(String version) implements ToolInstaller {
    public static final String DEFAULT_VERSION = "3.9.7";

    public static void main(String... args) {
      var version = System.getProperty("version", DEFAULT_VERSION);
      new Installer(version).install().run(args.length == 0 ? new String[] {"--version"} : args);
    }

    public Installer() {
      this(DEFAULT_VERSION);
    }

    @Override
    public ToolProvider install(Path into) throws Exception {
      var base = CENTRAL_REPOSITORY + "/org/apache/maven";
      var mavenWrapperProperties = into.resolve("maven-wrapper.properties");
      if (!Files.exists(mavenWrapperProperties))
        try {
          Files.writeString(
              mavenWrapperProperties,
              // language=properties
              """
              distributionUrl=%s/apache-maven/%s/apache-maven-%s-bin.zip
              """
                  .formatted(base, version, version));
        } catch (Exception exception) {
          throw new RuntimeException(exception);
        }
      var uri =
          URI.create(base + "/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar#SIZE=63028");
      var mavenWrapperJar = into.resolve("maven-wrapper.jar");
      download(mavenWrapperJar, uri);
      return ToolProgram.findJavaDevelopmentKitTool(
              "java",
              "-D" + "maven.multiModuleProjectDirectory=.",
              "--class-path=" + mavenWrapperJar,
              "org.apache.maven.wrapper.MavenWrapperMain")
          .orElseThrow();
    }
  }
}
