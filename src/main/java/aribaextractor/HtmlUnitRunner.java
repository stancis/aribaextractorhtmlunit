package aribaextractor;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;

public class HtmlUnitRunner {
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
      Properties properties = getProperties();

      //Run extractor
      Extractor extractor = new Extractor(webClient, properties.getProperty("url"));
      extractor.login(properties.getProperty("user"), properties.getProperty("password"));
      extractor.goToExportTab(properties.getProperty("reportLocation"));
      WebResponse response = extractor.exportReport(properties.getProperty("reportName").trim());
      extractor.logout();

      //Print retrieved reports
      ReportPrinter reportPrinter = new ReportPrinter(response);
      reportPrinter.print();
    } finally {
      webClient.close();
    }
    System.out.println("Execution took: " + (System.currentTimeMillis() - l) / 1000f + " seconds");
  }

  private static Properties getProperties() throws IOException {
    Properties properties = new Properties();
    properties.load(new FileReader("config.properties"));
    return properties;
  }
}