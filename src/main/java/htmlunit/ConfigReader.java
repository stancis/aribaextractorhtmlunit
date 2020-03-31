package htmlunit;

import secret.Encryptor;

import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigReader {
  private static final Set<String> PLAINTEXT_PROPERTIES = new HashSet<>(Arrays.asList("reportName", "reportLocation"));

  /**
   * Read configuration properties and decrypts encrypted values if keystore location and passwords are provided.
   *
   * @param configLocation   configuration file location
   * @param keystoreLocation keystore location
   * @param keystorePass     keystore password
   * @return plaintext properties map
   * @throws IOException
   * @throws GeneralSecurityException
   */
  public static Map<String, String> getProperties(String configLocation, String keystoreLocation, String keystorePass) throws IOException, GeneralSecurityException {
    Properties properties = new Properties();
    properties.load(new FileReader(configLocation));

    if (keystoreLocation == null) {
      return properties.entrySet().stream().collect(
          Collectors.toMap(e -> (String) e.getKey(), e -> (String) e.getValue()));
    }

    Encryptor encryptor = new Encryptor(keystoreLocation, keystorePass);
    return properties.entrySet().stream().collect(
        Collectors.toMap(e -> (String) e.getKey(), e -> {
          String value = (String) e.getValue();
          return PLAINTEXT_PROPERTIES.contains(e.getKey()) ? value : encryptor.decrypt(value);
        }));
  }
}
