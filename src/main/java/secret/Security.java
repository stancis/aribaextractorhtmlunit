package secret;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Helper class to encrypt and decrypt text and output it to stdout
 */
public class Security {
  public static void main(String[] args)
      throws IOException, GeneralSecurityException {
    if (args.length < 3) {
      System.out.println("Please provide operation (enc/dec), value, keystore location and keystore password if keystore is password-protected");
      System.out.println("For example: java -jar file.jar enc myValue keystore.p12 pass");
      return;
    }

    String operation = args[0];
    String value = args[1];
    String keystoreLocation = args[2];
    String keystorePass = args.length > 3 ? args[3] : null;
    Encryptor encryptor = new Encryptor(keystoreLocation, keystorePass);
    if ("dec".equalsIgnoreCase(operation)) {
      System.out.println(encryptor.decrypt(value));
    } else if ("enc".equalsIgnoreCase(operation)) {
      System.out.println(encryptor.encrypt(value));
    } else {
      System.out.println("Unsupported operation: " + operation);
    }
  }
}