package jdk.tool;

import java.io.Serial;

/** An unchecked exception thrown when a tool could not be found. */
public class ToolNotFoundException extends RuntimeException {
  @Serial private static final long serialVersionUID = 6739013019754028747L;

  /** Constructs exception with the specified detail message. */
  public ToolNotFoundException(String message) {
    super(message);
  }
}
