package htmlunit;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.logging.Level;

public class HtmlUnitRunner {
  /**
   * * If no arguments passed - plaintext config is expected
   * * If passed keystore location - AES key is read from provided keystore and used to decrypt config. Keystore must not
   * be password protected
   * * If passed keystore location and keystore password - AES key is read from provided keystore and used to decrypt config.
   * Keystore must be password protected
   *
   * @param args keystore location and keystore password. Both optional
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
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
      Map<String, String> properties;
      try {
        properties = ConfigReader.getProperties("config.properties", args.length > 0 ? args[0] : null, args.length > 1 ? args[1] : null);
      } catch (GeneralSecurityException | IOException e) {
        System.out.println("Failed to read to config. " + e.getMessage());
        return;
      }

      //Run extractor
      Extractor extractor = new Extractor(webClient, properties.get("endpoint"));
      extractor.login(properties.get("client"), properties.get("sharedSecret"));
      extractor.goToExportTab(properties.get("administrationType"), properties.get("reportLocation"));
      WebResponse response = extractor.exportReport(properties.get("reportName").trim());
      extractor.logout();

      //Print retrieved reports
      ReportPrinter reportPrinter = new ReportPrinter(response);
      reportPrinter.print();
    } finally {
      webClient.close();
    }
    System.out.println("Execution took: " + (System.currentTimeMillis() - l) / 1000f + " seconds");
  }
}