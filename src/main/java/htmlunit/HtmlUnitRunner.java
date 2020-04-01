package htmlunit;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class HtmlUnitRunner {
  /**
   * * If no arguments passed - plaintext config is expected
   * * If passed keystore location and keystore password - AES key is read from provided keystore and used to decrypt config.
   * Keystore must be password protected
   *
   * @param args keystore location and keystore password
   */
  public static void main(String[] args) {
    String keystoreLocation = args.length > 0 ? args[0] : null;
    String keystorePass = args.length > 1 ? args[1] : null;
    if (keystoreLocation != null && keystorePass == null) {
      handleError("Keystore password is missing");
    }

    long l = System.currentTimeMillis();
    //Disable logging
    java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);

    WebClient webClient = null;
    try {
      //Initialize HTML Unit web client
      webClient = new WebClient(BrowserVersion.INTERNET_EXPLORER);
      webClient.getOptions().setJavaScriptEnabled(true);
      webClient.setAjaxController(new NicelyResynchronizingAjaxController());

      //Read properties
      Map<String, Object> properties;
      try {
        properties = ConfigReader.getProperties("config.properties", keystoreLocation, keystorePass);
      } catch (GeneralSecurityException | IOException e) {
        System.out.println("Failed to read to config. " + e.getMessage());
        return;
      }

      //Run extractor
      Extractor extractor = new Extractor(webClient, (String) properties.get("endpoint"));
      extractor.login((String) properties.get("client"), (String) properties.get("sharedSecret"));
      extractor.goToAdministration((String) properties.get("administrationType"));

      List<Pair<String, String>> reports = (List<Pair<String, String>>) properties.get("reports");
      List<Pair<String, InputStream>> reportData = new ArrayList<>();
      for (Pair<String, String> report : reports) {
        extractor.goToExportTab(report.getValue());
        WebResponse response = extractor.exportReport(report.getKey().trim());
        reportData.add(Pair.of(response.getResponseHeaderValue("Content-Disposition").split("\"")[1],
            response.getContentAsStream()));
      }

      extractor.logout();

      //Print all retrieved reports
      initiatePrinting(reportData);
    } catch (ExtractorException e) {
      handleError(e.getMessage());
    } catch (IOException e) {
      handleError("Target site not accessible. Please verify target website status or your internet connection.");
    } finally {
      webClient.close();
    }
    System.out.println("Execution took: " + (System.currentTimeMillis() - l) / 1000f + " seconds");
  }

  private static void handleError(String error) {
    System.out.println(error);
    System.exit(-1);
  }

  private static void initiatePrinting(List<Pair<String, InputStream>> reports) {
    ReportPrinter reportPrinter = new ReportPrinter();
    for (Pair<String, InputStream> report : reports) {
      String reportName = report.getKey();
      try {
        if (isZip(reportName)) {
          try (ZipInputStream zis = new ZipInputStream(report.getValue());
               InputStreamReader reader = new InputStreamReader(zis)) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
              if (isCsv(zipEntry.getName())) {
                reportPrinter.printCsv(reader, zipEntry.getName());
              }
            }
          }
        } else if (isCsv(reportName)) {
          try (InputStreamReader reader = new InputStreamReader(report.getValue())) {
            reportPrinter.printCsv(reader, reportName);
          }
        }
      } catch (IOException e) {
        handleError(reportName + ": Failed to print CSV file in report");
      }
    }
  }

  private static boolean isZip(String filename) {
    return "zip".equalsIgnoreCase(FilenameUtils.getExtension(filename));
  }

  private static boolean isCsv(String fileName) {
    return "csv".equalsIgnoreCase(FilenameUtils.getExtension(fileName));
  }
}