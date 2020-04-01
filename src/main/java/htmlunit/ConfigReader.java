package htmlunit;

import org.apache.commons.lang3.tuple.Pair;
import secret.Encryptor;

import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class ConfigReader {
  public static final String REPORT_NAME = "reportName.";
  public static final String REPORT_LOCATION = "reportLocation.";
  private static final Set<String> REPORT_PROPERTIES = new HashSet<>(Arrays.asList(REPORT_NAME, REPORT_LOCATION));

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
  public static Map<String, Object> getProperties(String configLocation, String keystoreLocation, String keystorePass) throws IOException, GeneralSecurityException {
    Properties properties = new Properties();
    properties.load(new FileReader(configLocation));

    Encryptor encryptor = keystoreLocation == null ? null : new Encryptor(keystoreLocation, keystorePass);

    Map<String, Object> result = new HashMap<>();
    String[] reportNames = new String[5];
    String[] reportLocations = new String[5];
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      String key = (String) entry.getKey();
      String value = (String) entry.getValue();
      if (isReportProperty(key)) {
        String[] keyParts = key.split("\\.");
        if (key.startsWith(REPORT_NAME)) {
          reportNames[Integer.parseInt(keyParts[1]) - 1] = value;
        } else {
          reportLocations[Integer.parseInt(keyParts[1]) - 1] = value;
        }
      } else {
        result.put(key, encryptor == null ? value : encryptor.decrypt(value));
      }
    }

    List<Pair<String, String>> reports = new ArrayList<>();
    for (int i = 0; i < reportNames.length; i++) {
      String reportName = reportNames[i];
      String reportLocation = reportLocations[i];
      if (isNotEmpty(reportName) && isNotEmpty(reportLocation)) {
        reports.add(Pair.of(reportName, reportLocation));
      }
    }
    result.put("reports", reports);

    return result;
  }

  private static boolean isReportProperty(String name) {
    return REPORT_PROPERTIES.stream().anyMatch(name::startsWith);
  }
}