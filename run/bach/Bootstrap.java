package run.bach;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.spi.ToolProvider;

/**
 * Bach's Bootstrap Program.
 *
 * <ul>
 *   <li>{@code java @.bach/src/run.bach/run/bach/Bootstrap.java [<args...>]}
 *   <li>{@code java @bach <tool> [<args...>]}
 * </ul>
 */
class Bootstrap {
  public static void main(String... args) throws Exception {
    createDirectoriesAndFiles();
    System.exit(compileModules(Path.of(".bach/src")));
  }

  private static void createDirectoriesAndFiles() throws Exception {
    var logDirectory = Files.createDirectories(Path.of(".bach", "var", "log"));
    Files.writeString(
            logDirectory.resolve("bootstrap.log"),
            """
            instant=%s
            """.formatted(Instant.now()));

    var bachFile = Path.of("bach");
    if (Files.notExists(bachFile)) Files.writeString(bachFile, BACH_FILE_STRING);

    var gitignoreFile = Path.of(".bach", ".gitignore");
    if (Files.notExists(gitignoreFile)) Files.writeString(gitignoreFile, GIT_FILE_STRING);

    var loggingFile = Path.of(".bach", "logging.properties");
    if (Files.notExists(loggingFile)) Files.writeString(loggingFile, LOGGING_FILE_STRING);
  }

  private static int compileModules(Path directory) {
    var compile = new ArrayList<String>();

    compile.add("--module");
    compile.add(String.join(",", findModuleNames(directory)));

    compile.add("--module-source-path");
    compile.add(directory.toString());

    compile.add("-d");
    compile.add(Path.of(".bach/var/cache/bootstrap").toString());

    var javac = ToolProvider.findFirst("javac").orElseThrow();
    return javac.run(System.out, System.err, compile.toArray(String[]::new));
  }

  // return List.of("project", "run.bach");
  private static List<String> findModuleNames(Path folder) {
    if (!Files.isDirectory(folder)) return List.of();
    var names = new TreeSet<String>();
    try (var directories = Files.newDirectoryStream(folder, Files::isDirectory)) {
      for (var directory : directories) {
        var info = directory.resolve("module-info.java");
        if (Files.isRegularFile(info)) names.add(directory.getFileName().toString());
      }
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
    return List.copyOf(names);

  }

  private static final String BACH_FILE_STRING =
      """
      #
      # Java Launcher Argument File running module "run.bach"
      #
      
      #
      # Common debug-related arguments
      #
      # --show-version
      # --show-module-resolution
      
      #
      # Logging Properties
      #
      -Djava.util.logging.config.file=.bach/logging.properties
      
      #
      # Java Flight Recorder arguments
      #
      -Xlog:jfr+startup=error
      -XX:StartFlightRecording:name=Bach,filename=.bach/var/log/recording.jfr,dumponexit=true
      
      #
      # Path to all application modules
      #
      --module-path .bach/var/cache/bootstrap
      
      #
      # Set of additional root modules
      #
      --add-modules ALL-DEFAULT,ALL-MODULE-PATH
      
      #
      # Module to launch
      #
      --module run.bach/run.bach.Main
      """;

  private static final String GIT_FILE_STRING =
      """
      /out/
      /tmp/
      /var/
      """;

  private static final String LOGGING_FILE_STRING =
      """
      .level=INFO
      jdk.tool.level=ALL
      run.bach.level=ALL
            
      handlers=java.util.logging.ConsoleHandler,java.util.logging.FileHandler
            
      java.util.logging.ConsoleHandler.level=INFO
      java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter
            
      java.util.logging.FileHandler.level=ALL
      java.util.logging.FileHandler.formatter=java.util.logging.XMLFormatter
      java.util.logging.FileHandler.pattern=.bach/var/log/logging.xml
            
      java.util.logging.SimpleFormatter.format=%4$s %2$s %5$s%6$s%n
      """;
}
