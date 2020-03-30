package htmlunit;

import secret.Encryptor;

import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class ConfigReader {
  public static Map<String, String> getProperties(String configLocation, String keystoreLocation, String keystorePass) throws IOException, GeneralSecurityException {
    Properties properties = new Properties();
    properties.load(new FileReader(configLocation));

    if (keystoreLocation == null) {
      return properties.entrySet().stream().collect(
          Collectors.toMap(e -> (String) e.getKey(), e -> (String) e.getValue()));
    }

    Encryptor encryptor = new Encryptor(keystoreLocation, keystorePass);
    return properties.entrySet().stream().collect(
        Collectors.toMap(e -> (String) e.getKey(), e -> encryptor.decrypt((String) e.getValue())));
  }
}
