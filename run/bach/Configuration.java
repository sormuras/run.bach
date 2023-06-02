package run.bach;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.MODULE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Configuration {
  boolean enabled() default true;

  Toolbox toolbox() default @Toolbox;

  @Retention(RetentionPolicy.RUNTIME)
  @interface Toolbox {
    boolean withLoadingToolProviderServices() default false;

    String[] tools() default {"java"};

    Task[] tasks() default {};

    @interface Call {
      String tool();

      String[] args() default {};
    }

    @interface Task {
      String MODULE_NAME_AND_TASK = "NAME_OF_ANNOTATED_MODULE/task";

      String namespace() default MODULE_NAME_AND_TASK;

      String name();

      String version() default "";

      boolean parallel() default false;

      Call[] calls() default {};
    }

    enum DirectoryBase {
      CURRENT_WORKING_DIRECTORY,
      USER_HOME_DIRECTORY,
      TEMPORARY_DIRECTORY
    }

    DirectoryBase withInstallationDirectoryBase() default DirectoryBase.CURRENT_WORKING_DIRECTORY;

    String withInstallationDirectory() default ".bach/var/cache/toolbox";

    Tool[] withInstallingTools() default {};

    @interface Tool {
      String name();

      String version();
    }
  }
}
