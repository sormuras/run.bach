package run.bach;

import static java.lang.System.Logger.Level.DEBUG;

import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;
import jdk.tool.Tool;
import jdk.tool.ToolCall;
import jdk.tool.ToolInstaller;
import jdk.tool.Toolbox;

record Configurator() {
  private static final System.Logger logger = System.getLogger(Configurator.class.getName());

  Bach configureBach(Toolbox toolbox) {
    logger.log(DEBUG, "Configuring Bach...");
    for (var module : Bach.class.getModule().getLayer().modules()) {
      for (var configuration : module.getAnnotationsByType(Configuration.class)) {
        logger.log(DEBUG, "Parsing configuration of {0}", module);

        if (!configuration.enabled()) {
          logger.log(DEBUG, "Skipping disabled configuration of {0}", module);
          continue;
        }

        logger.log(DEBUG, "Converting task annotations into tool instances");
        for (var task : configuration.toolbox().tasks()) {
          var namespace =
              Configuration.Toolbox.Task.MODULE_NAME.equals(task.namespace())
                  ? module.getName()
                  : task.namespace();
          var name = task.name();
          var version = task.version().isBlank() ? null : task.version();
          var tool = Tool.of(namespace, name, version, newTask(task));
          toolbox = toolbox.withTool(tool);
        }

        if (configuration.toolbox().withLoadingToolProviderServices()) {
          logger.log(DEBUG, "Loading all tool provider service implementations");
          toolbox = toolbox.withToolProviderServices();
        }

        logger.log(DEBUG, "Finding tools by name, including programs in Java's home directory");
        for (var name : configuration.toolbox().tools()) {
          var tool = Tool.of(name);
          toolbox = toolbox.withTool(tool);
        }

        {
          logger.log(DEBUG, "Installing external tools");
          var base =
              switch (configuration.toolbox().withInstallationDirectoryBase()) {
                case CURRENT_WORKING_DIRECTORY -> Path.of("");
                case USER_HOME_DIRECTORY -> Path.of(System.getProperty("user.home"));
                case TEMPORARY_DIRECTORY -> Path.of(
                    System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
              };
          var directory = base.resolve(configuration.toolbox().withInstallationDirectory());
          var installer = new ToolInstaller.Installer(directory);
          for (var tool : configuration.toolbox().withInstallingTools()) {
            var installed = installer.install(tool.name(), tool.version());
            toolbox = toolbox.withTools(installed);
          }
        }
      }
    }
    logger.log(DEBUG, "Configured Bach with {0} tools", toolbox.tools().size());
    return new Bach(toolbox);
  }

  Task newTask(Configuration.Toolbox.Task task) {
    return new Task(
        task.name(),
        Stream.of(task.calls()).map(call -> ToolCall.of(call.tool()).with(call.args())).toList(),
        task.parallel());
  }
}
