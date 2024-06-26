/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach;

import static run.bach.ToolCall.Carrier.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.System.Logger.Level;
import java.util.EnumSet;
import java.util.Set;
import run.bach.internal.FlightRecorderEvent;

/** Extendable tool runner implementation. */
public class ToolSpace implements ToolRunner {
  public enum Flag {
    SILENT
  }

  protected final ToolFinder finder;
  protected final Level threshold;
  protected final Set<Flag> flags;

  public ToolSpace(Flag... flags) {
    this(ToolFinder.ofSystem(), Level.INFO, flags);
  }

  public ToolSpace(ToolFinder finder, Flag... flags) {
    this(finder, Level.INFO, flags);
  }

  public ToolSpace(ToolFinder finder, Level threshold, Flag... flags) {
    this.finder = finder;
    this.threshold = threshold;
    this.flags =
        switch (flags.length) {
          case 0 -> EnumSet.noneOf(Flag.class);
          case 1 -> EnumSet.of(flags[0]);
          default -> EnumSet.of(flags[0], flags);
        };
  }

  public final boolean silent() {
    return flags.contains(Flag.SILENT);
  }

  @Override
  public ToolRun run(ToolCall call) {
    announce(call);
    var event = new FlightRecorderEvent.ToolRunEvent();
    try {
      var tool = computeToolInstance(call);
      var args = computeArgumentsArray(call);
      var out = new StringPrintWriterMirror(computePrintWriter(Level.INFO));
      var err = new StringPrintWriterMirror(computePrintWriter(Level.ERROR));
      var provider = tool.provider();

      event.name = call.tool().name();
      event.tool = provider.getClass();
      event.args = String.join(" ", args);

      Thread.currentThread().setContextClassLoader(provider.getClass().getClassLoader());
      try {
        event.begin();
        event.code = provider.run(out, err, args);
      } catch (RuntimeException unchecked) {
        event.code = Integer.MIN_VALUE;
        throw unchecked;
      } finally {
        event.end();
        event.out = out.toString();
        event.err = err.toString();
      }

      var run = new ToolRun(call, tool, event.code, event.out, event.err);
      verify(run);

      return run;
    } finally {
      event.commit();
    }
  }

  @Override
  public void log(Level level, String message) {
    // TODO Fire flight recorder event.
    if (silent()) return;
    var severity = level.getSeverity();
    if (severity < threshold.getSeverity()) return;
    if (severity < Level.ERROR.getSeverity()) {
      System.out.println(message);
    } else {
      System.err.println(message);
    }
  }

  protected void announce(ToolCall call) {
    log(Level.INFO, "| " + call.toCommandLine());
  }

  protected PrintWriter computePrintWriter(Level level) {
    if (silent() || level == Level.OFF) {
      return new PrintWriter(Writer.nullWriter());
    }
    var severity = level.getSeverity();
    var stream = severity < Level.ERROR.getSeverity() ? System.out : System.err;
    return new PrintWriter(stream, true);
  }

  protected Tool computeToolInstance(ToolCall call) {
    return switch (call.tool()) {
      case ByName(String name) -> finder.get(name);
      case Direct(Tool tool) -> tool;
    };
  }

  protected String[] computeArgumentsArray(ToolCall call) {
    return call.arguments().toArray(String[]::new);
  }

  protected void verify(ToolRun run) {
    var code = run.code();
    if (code == 0) return;
    var name = run.call().tool().name();
    throw new RuntimeException("%s finished with exit code %d".formatted(name, code));
  }

  private static class StringPrintWriter extends PrintWriter {
    StringPrintWriter() {
      super(new StringWriter());
    }

    @Override
    public String toString() {
      return super.out.toString().stripTrailing();
    }
  }

  private static class StringPrintWriterMirror extends StringPrintWriter {
    private final PrintWriter other;

    StringPrintWriterMirror(PrintWriter other) {
      this.other = other;
    }

    @Override
    public void flush() {
      super.flush();
      other.flush();
    }

    @Override
    public void write(int c) {
      super.write(c);
      other.write(c);
    }

    @Override
    public void write(char[] buf, int off, int len) {
      super.write(buf, off, len);
      other.write(buf, off, len);
    }

    @Override
    public void write(String s, int off, int len) {
      super.write(s, off, len);
      other.write(s, off, len);
    }

    @Override
    public void println() {
      super.println();
      other.println();
    }
  }
}
