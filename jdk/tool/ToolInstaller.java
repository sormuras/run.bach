package jdk.tool;

import static java.lang.System.Logger.Level.DEBUG;

import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.List;
import java.util.ServiceLoader;

/** Knows how to create tool instances by fetching external tool assets into a local folder. */
public interface ToolInstaller {

  default String namespace() {
    return Tools.computeDefaultNamespace(this);
  }

  String name();

  List<Tool> install(Path folder, String version) throws Exception;

  static Installer installer(String directory) {
    return new Installer(Path.of(directory));
  }

  static Path download(URI source, Path target) {
    if (Files.exists(target)) return target;
    try (var stream = source.toURL().openStream()) {
      Files.createDirectories(target.getParent());
      Files.copy(stream, target);
      return target;
    } catch (Exception exception) {
      throw new RuntimeException("Download failed: " + source, exception);
    }
  }

  record Installer(Path directory) {
    private static final System.Logger logger = System.getLogger(Installer.class.getName());

    public List<Tool> install(String name, String version) {
      var installers =
          ServiceLoader.load(ToolInstaller.class, getClass().getClassLoader()).stream()
              .map(ServiceLoader.Provider::get)
              .filter(installer -> installer.name().equals(name))
              .toList();
      if (installers.isEmpty()) throw new RuntimeException("No tool installer found for: " + name);
      return install(installers.get(0), version);
    }

    public List<Tool> install(ToolInstaller installer, String version) {
      return install(installer, directory, version);
    }

    public List<Tool> install(ToolInstaller installer, Path directory, String version) {
      return installNow(installer, directory, version);
    }

    List<Tool> installNow(ToolInstaller installer, Path directory, String version) {
      var namespace = installer.namespace();
      var name = installer.name();
      try {
        var nameAndVersion = name + '@' + version;
        var folder = directory.resolve(namespace).resolve(nameAndVersion);
        var lock = folder.resolve(".lock");
        installation:
        while (true) {
          try (var event = ToolInstallationEvent.begin(installer, version, folder)) {
            Files.createDirectories(folder);
            Files.createFile(lock);
            logger.log(DEBUG, "Installing {0} into folder {1}", nameAndVersion, folder.toUri());
            var installed = installer.install(folder, version);
            event.installedSize = installed.size();
            event.installedTools = Tools.toString(installed);
            return installed;
          } catch (FileAlreadyExistsException exception) {
            try (var watcher = folder.getFileSystem().newWatchService()) {
              var key = folder.register(watcher, StandardWatchEventKinds.ENTRY_DELETE);
              while (key.isValid()) {
                for (var event : key.pollEvents()) {
                  var kind = event.kind();
                  if (kind == StandardWatchEventKinds.OVERFLOW) continue;
                  @SuppressWarnings("unchecked")
                  var path = ((WatchEvent<Path>) event).context();
                  if (path.equals(lock)) continue installation;
                }
                key.reset();
              }
            }
          } finally {
            Files.deleteIfExists(lock);
          }
        }
      } catch (Exception exception) {
        throw new RuntimeException("Installation failed: " + name, exception);
      }
    }
  }
}
