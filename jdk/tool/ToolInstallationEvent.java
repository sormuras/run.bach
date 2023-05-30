package jdk.tool;

import java.nio.file.Path;
import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Category({"Java Development Kit", "Tool", "Tool Installation"})
@StackTrace(false)
@Label("Tool Installation")
@Name("jdk.tool.ToolInstallation")
@Description("Describes a tool installation")
final class ToolInstallationEvent extends Event implements AutoCloseable {
  static ToolInstallationEvent begin(ToolInstaller installer, String version, Path folder) {
    var event = new ToolInstallationEvent();
    if (event.isEnabled()) {
      event.namespace = installer.namespace();
      event.name = installer.name();
      event.version = version;
      event.folder = folder.toUri().toString();
      event.begin();
    }
    return event;
  }

  @Override
  public void close() {
    if (shouldCommit()) commit();
  }

  String namespace;

  String name;

  String version;

  String folder;

  int installedSize;

  String installedTools;
}
