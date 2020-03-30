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
    HtmlElement usernameInput = (HtmlElement) page.getElementById("UserName");
    usernameInput.type(client);
    HtmlElement passwordInput = (HtmlElement) page.getElementById("Password");
    passwordInput.type(sharedSecret);
    page = page.<HtmlElement>querySelector("td.w-login-form-input-btn-space > .w-login-form-btn").click();
    System.out.println("Target System Connected");
  }

  public void goToExportTab(String administrationType, String reportLocation) throws IOException {
    String manageSelector = "td.a-nav-bar-manage > a";
    waitCss(manageSelector);
    page = page.<HtmlElement>querySelector(manageSelector).click();
    page = page.<HtmlElement>querySelector("#Manage a[title='" + administrationType + "']").click();

    String reportLocationXpath = "//a[@title='" + reportLocation + "']";
    waitXpath(reportLocationXpath);
    List<HtmlElement> firstMenu = page.getByXPath(reportLocationXpath + "/ancestor::tr[1]/following-sibling::tr[2]");
    if (firstMenu.isEmpty() || !firstMenu.get(0).getAttribute("class").contains("tocItem")) {
      page = page.<HtmlElement>getFirstByXPath(reportLocationXpath).click();
    }
    waitXpath(reportLocationXpath + "/ancestor::tr[1]/following-sibling::tr[2][contains(@class, 'tocItem')]");
    page = page.<HtmlElement>getByXPath(reportLocationXpath + "/ancestor::tr[1]/following-sibling::tr[contains(@class, 'tocItem')]" +
        "//a[contains(@title, 'Data Import/Export')]").get(0).click();

    String exportTabXpath = "//a[text()='Export']";
    waitXpath(exportTabXpath);
    webClient.waitForBackgroundJavaScriptStartingBefore(5000);//JavaScript is taking longer here, adding max delay
    page = page.<HtmlElement>getFirstByXPath(exportTabXpath).click();
    System.out.println("Identifying Export");
  }

  public WebResponse exportReport(String reportName) throws IOException {
    URL url = page.getUrl();
    String exportButtonXpath = "//td[contains(text(), '" + reportName + "')]/" +
        "ancestor::tr[contains(@class, 'tableRow1')]//button";
    waitXpath(exportButtonXpath);

    page = page.<HtmlElement>getFirstByXPath(exportButtonXpath).click();
    String okXpath = "//div[@class='w-dlg-buttons']/button/span[contains(text(), 'OK')]";
    waitXpath(okXpath);
    page = page.<HtmlElement>getFirstByXPath(okXpath).click();

    waitUntil(() -> page.getElementById("AWDownload") != null);
    System.out.println("Download Initiated...");
    URL downloadUrl = page.getFullyQualifiedUrl(page.getElementById("AWDownload").getAttribute("src"));
    WebResponse response = webClient.getPage(downloadUrl).getWebResponse();
    page = webClient.getPage(url);
    System.out.println("Download Completed");
    return response;
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