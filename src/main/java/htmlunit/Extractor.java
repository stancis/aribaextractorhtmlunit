package htmlunit;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.function.Supplier;

public class Extractor {
  private static final int TIMEOUT = 15000;
  private static final long WAIT_TIME = 250;
  private WebClient webClient;
  private HtmlPage page;

  public Extractor(WebClient webClient, String endpoint) throws IOException {
    this.webClient = webClient;
    this.page = webClient.getPage(endpoint);
  }

  public void login(String client, String sharedSecret) throws IOException {
    HtmlElement passwordInput = (HtmlElement) page.getElementById("Password");
    if (passwordInput == null) {
      throw new ExtractorException("Target site under maintenance. Please verify target website status.");
    }//Logic: Not able to find passwordInput on the target page

    HtmlElement usernameInput = (HtmlElement) page.getElementById("UserName");
    usernameInput.type(client);
    passwordInput.type(sharedSecret);
    page = page.<HtmlElement>querySelector("td.w-login-form-input-btn-space > .w-login-form-btn").click();

    if (page.getElementById("Password") != null) {
      throw new ExtractorException("Client and/or sharedsecret credential error. Please verify the combination of your credentials with Ariba Administrator");
    }//Logic: Password Input field still show after click login

    System.out.println("Target System Connected");
  }

  public void goToExportTab(String administrationType, String reportLocation) throws IOException {
    String manageSelector = "td.a-nav-bar-manage > a"; 
    //Key element class attributes we do not rely on manage title alone
    waitCss(manageSelector);
    HtmlElement manageConsole = page.querySelector(manageSelector);
    if (manageConsole == null) {
      throw new ExtractorException("Cannot access \"manage\" console. Please contact your Ariba Administrator to check your credential privilege.");
    }
    //Logic: manageConsole not visible, meaning you can't access the manage console
    page = manageConsole.click();

    HtmlElement administrationConsole = page.querySelector("#Manage a[title='" + administrationType + "']");
    if (administrationConsole == null) {
      throw new ExtractorException("Cannot access \"" + administrationType + "\" console. Please contact your Ariba Administrator to check your credential privilege.");
    }//Logic: Need to have the right Administration type otherwise not authorised, meaning that the priviledge is not assigned.
    page = administrationConsole.click();

    try {
      String reportLocationXpath = "//a[@title='" + reportLocation + "']";
      //define the pattern to look on the page. Logic: must contain title with value as reportlocation from properties
      waitXpath(reportLocationXpath);
      //instruct the above stream to wait for the full appearance of reportlocation title
      List<HtmlElement> firstMenu = page.getByXPath(reportLocationXpath + "/ancestor::tr[1]/following-sibling::tr[2]");
      //get the ancestor row in order to identify the sibling row for reporting element. No class elements here
      if (firstMenu.isEmpty() || !firstMenu.get(0).getAttribute("class").contains("tocItem")) {
        page = page.<HtmlElement>getFirstByXPath(reportLocationXpath).click();
      } //check if the element exists, if so click
      waitXpath(reportLocationXpath + "/ancestor::tr[1]/following-sibling::tr[2][contains(@class, 'tocItem')]"); //wait for full apperance for the sibiling rows
      page = page.<HtmlElement>getByXPath(reportLocationXpath + "/ancestor::tr[1]/following-sibling::tr[contains(@class, 'tocItem')]" +
          "//a[contains(@title, 'Data Import/Export')]").get(0).click();//get and click

      String exportTabXpath = "//a[text()='Export']";
      //look for a link tab contains "Export" as text
      waitXpath(exportTabXpath);
      webClient.waitForBackgroundJavaScriptStartingBefore(5000);
      //JavaScript is taking longer here, adding max delay for 5000 ms
      page = page.<HtmlElement>getFirstByXPath(exportTabXpath).click();
      System.out.println("Identifying Export");
    } catch (RuntimeException e) {
      throw new ExtractorException("Cannot access specific report requested. Please contact your Ariba Administrator to verify UI change", e);
    } //in case we can't reach this report due to UI change, throw an error here
  }

  public WebResponse exportReport(String reportName) throws IOException {
    URL url = page.getUrl();
    //check and copy current URL. After downloading the data we can restore the current position
    String exportButtonXpath = "//td[contains(text(), '" + reportName + "')]/" +
        "ancestor::tr[contains(@class, 'tableRow1')]//button";
    //call report name from properties
    waitXpath(exportButtonXpath);
    
    HtmlElement exportButton = page.getFirstByXPath(exportButtonXpath);
    //find export button for the corresponding report
    if (exportButton == null) {
      throw new ExtractorException("Cannot access specific report requested. Please contact your Ariba Administrator to check your credential reporting privilege.");
    }//LOGIC: If export button does not exist it indicates the account missed priviledge
    page = exportButton.click();

    try {
      String okXpath = "//div[@class='w-dlg-buttons']/button/span[contains(text(), 'OK')]";
      waitXpath(okXpath);
      //Handle pop up dialogue to click OK
      page = page.<HtmlElement>getFirstByXPath(okXpath).click();

      waitUntil(() -> page.getElementById("AWDownload") != null);
      //AWDownload element will appear on the page. Use this element as the key attributes
      System.out.println("Download Initiated...");
      URL downloadUrl = page.getFullyQualifiedUrl(page.getElementById("AWDownload").getAttribute("src"));
      WebResponse response = webClient.getPage(downloadUrl).getWebResponse();
      page = webClient.getPage(url);
      System.out.println("Download Completed");
      return response;
    } catch (RuntimeException e) {
      throw new ExtractorException("Access to specific report is granted. Report download failed. Please contact your site administrator", e);
    } //LOGIC: download runtime exception means report download failed
  }

  public void logout() throws IOException {
    waitCss("a.awmenuLink[_mid=Preferences]");
    page = page.<HtmlElement>querySelector("a.awmenuLink[_mid=Preferences]").click();
    page = ((HtmlElement) page.querySelectorAll("#Preferences a").get(0)).click();
    System.out.println("Target System Disconnected");
  }

  private void refresh() {
    page = (HtmlPage) page.getEnclosingWindow().getEnclosedPage();
  }

  private void waitCss(String cssSelector) {
    waitToBeVisible(() -> page.querySelector(cssSelector));
  }

  private void waitXpath(String xpath) {
    waitToBeVisible(() -> page.getFirstByXPath(xpath));
  }

  private void waitToBeVisible(Supplier<HtmlElement> elementSupplier) {
    waitUntil(() -> {
      HtmlElement element = elementSupplier.get();
      return element != null && element.isDisplayed();
    });
  }

  private void waitUntil(Supplier<Boolean> elementSupplier) {
    for (int i = 0; i < (long) TIMEOUT / WAIT_TIME; i++) {
      refresh();
      if (elementSupplier.get()) {
        return;
      }
      webClient.waitForBackgroundJavaScript(WAIT_TIME);
    }
  }
}