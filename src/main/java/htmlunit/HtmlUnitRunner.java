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
  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      System.out.println("Please provide keystore location and keystore password if keystore is password-protected");
      System.out.println("For example: java -jar file.jar keystore.p12 pass");
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
      Map<String, String> properties;
      try {
        properties = ConfigReader.getProperties("config.properties", args[0], args.length > 1 ? args[1] : null);
      } catch (GeneralSecurityException e) {
        System.out.println("Failed to read properties file. " + e.getMessage());
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