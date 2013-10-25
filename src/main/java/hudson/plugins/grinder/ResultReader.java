package hudson.plugins.grinder;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Class used to read results from a Grinder output file.
 *
 * @author Eivind B Waaler
 */
public class ResultReader {
   private List<Test> tests;
   private Test totals;

   private transient final PrintStream hudsonConsoleWriter;

   private static final String PATTERN_TEST = "Test \\d.*";
   private static final String PATTERN_TOTALS = "Totals .*";

   /**
    * Construct a result reader for grinder out log files.
    *
    * @param is     The input stream giving the out log file.
    * @param logger Logger to print messages to.
    * @throws GrinderParseException Thrown if the parsing fails.
    */
   public ResultReader(InputStream is, PrintStream logger) {
      hudsonConsoleWriter = logger;
      parse(is);
   }

   private void parse(InputStream is) {
      if (is == null) {
         throw new GrinderParseException("Empty input stream");
      }
      if (tests == null) {
         tests = new ArrayList<Test>();
      }
      try {
         Scanner scanner = new Scanner(IOUtils.toString(is));
         String match = scanner.findWithinHorizon(PATTERN_TEST, 0);
         while (match != null) {
            tests.add(readTest(match, false));
            match = scanner.findWithinHorizon(PATTERN_TEST, 0);
         }
         match = scanner.findWithinHorizon(PATTERN_TOTALS, 0);
         totals = readTest(match, true);
      } catch (IOException e) {
         String errMsg = "Problem parsing Grinder out log file";
         hudsonConsoleWriter.println(errMsg + ": " + e.getMessage());
         e.printStackTrace(hudsonConsoleWriter);
         throw new GrinderParseException(errMsg, e);
      }
   }

   public List<Test> getTests() {
      return tests;
   }

   public Test getTotals() {
      return totals;
   }

   private Test readTest(String testLine, boolean isTotals) {
      Scanner scanner = new Scanner(testLine).useDelimiter("\\s{2,}").useLocale(Locale.ENGLISH);
      String id = scanner.next();
      int testCount = scanner.nextInt();
      int errorCount = scanner.nextInt();
      double meanTestTime = scanner.nextDouble();
      double testStdDevTime = scanner.nextDouble();
      double meanRespLength = scanner.nextDouble();
      double respBytesPrSecond = 0.0;
      scanner.next(); // reported as '?' in log
      int respErrorCount = scanner.nextInt();
      double resolveHostMeanTime = scanner.nextDouble();
      double establishConnMeanTime = scanner.nextDouble();
      double firstByteMeanTime = scanner.nextDouble();
      String name = isTotals ? "" : scanner.next().replaceAll("\"", "");

      return new Test(
         id,
         testCount,
         errorCount,
         meanTestTime,
         testStdDevTime,
         meanRespLength,
         respBytesPrSecond,
         respErrorCount,
         resolveHostMeanTime,
         establishConnMeanTime,
         firstByteMeanTime,
         name
      );
   }
}
