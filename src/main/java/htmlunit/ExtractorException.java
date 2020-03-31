package htmlunit;

public class ExtractorException extends RuntimeException {
  public ExtractorException(String message) {
    super(message);
  }

  public ExtractorException(String message, Throwable cause) {
    super(message, cause);
  }
}
