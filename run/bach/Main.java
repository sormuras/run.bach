package run.bach;

import jdk.tool.Toolbox;

final class Main {
  public static void main(String... args) {
    try {
      var conf = new Configurator();
      var bach = conf.configureBach(Toolbox.ofSystemPrinter());
      var code = bach.run(args);
      System.exit(code);
    } catch (Throwable throwable) {
      throwable.printStackTrace(System.err);
      System.exit(-1);
    }
  }
}
