/** Defines Bach's API. */
module run.bach {
  requires java.logging;
  requires jdk.compiler;
  requires jdk.javadoc;
  requires jdk.jfr;

  exports run.bach;
  exports jdk.tool;

  uses java.util.spi.ToolProvider;
  uses jdk.tool.ToolInstaller;
}
