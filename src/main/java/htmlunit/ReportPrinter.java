package htmlunit;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

public class ReportPrinter {
  public void printCsv(InputStreamReader reader, String reportName) throws IOException {
    CSVParser parser = CSVFormat.DEFAULT.parse(reader);
    Iterator<CSVRecord> it = parser.iterator();
    it.next();//Skip UTF-8
    int columnCount = it.next().size();//Count columns and skip headers
    System.out.println("\nReport " + reportName);
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
}
