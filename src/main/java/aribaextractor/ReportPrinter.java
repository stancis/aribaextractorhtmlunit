package aribaextractor;

import com.gargoylesoftware.htmlunit.WebResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ReportPrinter {
  private WebResponse report;
  private String reportFileName;

  public ReportPrinter(WebResponse report) {
    this.report = report;
    reportFileName = report.getResponseHeaderValue("Content-Disposition").split("\"")[1];
  }

  public void print() throws IOException {
    if (isZip()) {
      try (ZipInputStream zis = new ZipInputStream(report.getContentAsStream());
           InputStreamReader reader = new InputStreamReader(zis)) {
        ZipEntry zipEntry;
        while ((zipEntry = zis.getNextEntry()) != null) {
          printCsv(reader, zipEntry.getName());
        }
      }
    } else {
      try (InputStreamReader reader = new InputStreamReader(report.getContentAsStream())) {
        printCsv(reader, reportFileName);
      }
    }
  }

  private void printCsv(InputStreamReader reader, String fileName) throws IOException {
    if (!isCsv(fileName)) {
      System.out.println("Not a CSV file, will not be printed: " + fileName);
      return;
    }
    CSVParser parser = CSVFormat.DEFAULT.parse(reader);
    Iterator<CSVRecord> it = parser.iterator();
    it.next();//Skip UTF-8
    int columnCount = it.next().size();//Count columns and skip headers
    System.out.println("\nReport " + fileName);
    if (columnCount > 1) {
      printMultiColumnReport(it);
    } else {
      printSingleColumnReport(it);
    }
  }

  private void printSingleColumnReport(Iterator<CSVRecord> it) {
    StringBuilder sb = new StringBuilder();
    it.forEachRemaining(record -> {
      if (record.getParser().getCurrentLineNumber() != 3) {
        sb.append(',');
      }
      sb.append(record.get(0));
    });
    System.out.println(sb.toString());
  }

  private void printMultiColumnReport(Iterator<CSVRecord> it) {
    it.forEachRemaining(record -> {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < record.size(); i++) {
        if (i > 0) {
          sb.append(',');
        }
        sb.append(record.get(i));
      }
      System.out.println(sb.toString());
    });
  }

  private boolean isZip() {
    return "zip".equalsIgnoreCase(FilenameUtils.getExtension(reportFileName));
  }

  private boolean isCsv(String fileName) {
    return "csv".equalsIgnoreCase(FilenameUtils.getExtension(fileName));
  }
}
