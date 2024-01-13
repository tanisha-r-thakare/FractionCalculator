import java.util.*;
import java.util.concurrent.*;
import java.io.*;

/**
 * Executes Unit Tests by loading a file with commands, calling processCommand()
 * and comparing the result with the expected results. It will sum up points. It
 * may have sub sections. It will generate a summary report at the end. Can
 * create unit test files by recording a session.
 *
 *
 */
public class UnitTestRunner {

   public interface CommandHandler {
      public String processCommand(String cmd);
   }

   private static PrintStream testFile = null;
   
   private static boolean callingProcess = false;

   /**
    * Processes the command if it is a Test command
    *
    * @param input The command string that the user entered to be processed.
    * @return true if the command was a Test command. This means that the caller
    *         should ignore the input. False means the caller should proceed to process
    *         the command.
    */
   public static boolean processCommand(String input, CommandHandler cmdHandler) {
      // avoid infinite recursion here
      if (callingProcess) {
         // don't handle the command.
         return false;
      }
      
      if (generateTestCommand(input) || runTestCommand(input, cmdHandler)) {
         return true;
      } else if (testFile != null) {
         // check for the "quit" command.
         if (input.equalsIgnoreCase("quit")) {
            // close the test file
            // return as not processed
            testFile.flush();
            testFile.close();
            testFile = null;
            return false;
         }

         // output the command to the file
         testFile.println(input);
      
         // get the output from the CommandHandler
         callingProcess = true;
         String result = cmdHandler.processCommand(input);
         if (result == null || result.length() == 0) {
            testFile.println("0");
         } else {
            // output the results to the file, but get line count first
            String[] answer = result.split("\\n");
            testFile.println(answer.length);
            // print one line at a time to make it look nicer in the file
            for (int line = 0; line < answer.length; line++) {
               testFile.println(answer[line]);
            }
            System.out.println(result);
         }
         // reset to allow entry
         callingProcess = false;
         
         return true;
      }
   
      // this was not a test command
      return false;
   }

   private static void runCheckpointTests(String checkpoint, boolean breakOnFail, CommandHandler cmdHandler) {
      String file = "tests_checkpoint" + checkpoint + ".txt";
      runTests(file, breakOnFail, cmdHandler);
   }

   /**
    * check if input is: test create #
    * 
    * if so, it will redirect output to a file. When done with the tests, type:
    * test off
    * 
    * @param input The command string that the user entered to be processed.
    * @return true if the command was processed
    */
   private static boolean generateTestCommand(String input) {
      // get the tokens
      boolean retValue = false;
      String tokens[] = input.split(" ");
      if (tokens.length < 2) {
         return retValue;
      }
   
      if ("test".equalsIgnoreCase(tokens[0])) {
         if ("create".equalsIgnoreCase(tokens[1]) && tokens.length > 2) {
            try {
               testFile = new PrintStream(new File("tests_checkpoint" + tokens[2] + ".txt"));
            } catch (Exception e) {
               System.out.println(e.getMessage());
               e.printStackTrace();
            }
            retValue = true;
         } else if (testFile != null && "end".equalsIgnoreCase(tokens[1])) {
            // creating the test is complete. Turn it off.
            // flush file and close it up.
            testFile.flush();
            testFile.close();
            testFile = null;
            retValue = true;
         }
      }
   
      return retValue;
   }

   /**
    * If the input is a Test command, it runs the series of tests found in that
    * file. The command is in the format:
    * <ul>
    * <li>test {checkpoint name/#} [boolean:break_on_fail]</li>
    * <li>Examples:
    * <ul>
    * <li>test 1</li>
    * <li>test final</li>
    * <li>test extra true</li>
    * </ul>
    * </li>
    * </ul>
    *
    * The [boolean:break_on_fail] is defaulted to FALSE.
    *
    *
    * @param input The command that the user input.
    * @return true if the was a Test command and tests were run
    */
   private static boolean runTestCommand(String input, CommandHandler cmdHandler) {
      // get the tokens
      Scanner parser = new Scanner(input);
      boolean retValue = false;
   
      if ("test".equalsIgnoreCase(parser.next())) {
         String checkpoint;
         
         // default to stopping on a test failing
         boolean breakOnFail = true;
      
         if (parser.hasNext()) {
            checkpoint = parser.next();
         
            // check for one optional boolean value
            if (parser.hasNextBoolean()) {
               breakOnFail = parser.nextBoolean();
            }
         
            // execute the tests
            runCheckpointTests(checkpoint, breakOnFail, cmdHandler);
         
            // tests were processed
            retValue = true;
         }
      }
   
      // avoid leaks, or at least warnings of leaks
      parser.close();
   
      return retValue;
   }

   /**
    * This will open a file and run the commands found inside of it. Subsequent
    * lines will contain the expected output of running the command. The format is:
    * <p>
    * &lt;command&gt; &lt;# of lines in expected output&gt; &lt;expected output, if
    * any&gt; Examples include:
    * <p>
    * a1 = 7 0 value a1 1 7.0
    *
    * @param filename    is the name of the test file
    * @param breakOnFail If true, stop at the first failure
    */
   private static void runTests(String filename, boolean breakOnFail, CommandHandler cmdHandler) {
      File f = null;
      Scanner file = null;
      try {
         // load the file of test cases
         f = new File(filename);
         file = new Scanner(f);
         ArrayList<String> summary = new ArrayList<>();
         runTests(file, breakOnFail, summary, cmdHandler);
         // print out the summary
         if (summary.size() > 1) {
            System.out.println("Summary Report:");
            for (String line : summary) {
               System.out.println(line);
            }
         }
      } catch (FileNotFoundException e) {
         System.out.println("Cannot find test file. Here are details:");
         if (f != null) {
            System.out.println(" path of file: " + f.getAbsolutePath());
         }
         System.out.println(e.getMessage());
      } finally {
         if (file != null) {
            file.close();
         }
      }
   }

   /**
    * Runs the tests in a file, breaking as necessary and adding to the summary
    * results.
    * 
    * @param file        The Scanner for the file to test
    * @param breakOnFail If true, then stop tests at first exception, otherwise,
    *                    keep on running the tests in the file
    * @param summary     The summary data as a list of strings
    * @return Total points earned
    */
   private static double runTests(Scanner file, boolean breakOnFail, ArrayList<String> summary, CommandHandler cmdHandler) {
   
      // all tests will be run using the CommandHandler
      if (cmdHandler == null) {
         System.out.println("No CommandHandler means No tests run.");
         return 0;
      }
   
      double points = 0;
      double total = 0.0;
      double subSectionPoints = 0;
      double subSectionTotal = 0;
   
      while (file.hasNextLine()) {
         try {
            String input = file.nextLine();
            if (input.toLowerCase().startsWith("<timeout")) {
               // let's create a new thread and run the tests
               // on a timeout. Regardless of whether we timeout,
               // or not, we will exit out tests and print our summary.
               // Get the timeout parameter. <timeoutStart time=100>"
               int startIndex = input.indexOf("=") + 1;
               int endIndex = input.indexOf(">");
               int timeout = Integer.parseInt(input.substring(startIndex, endIndex));
               // System.out.println("Calling timeout code");
               subSectionPoints = startTimeout(file, timeout, breakOnFail, cmdHandler);
               // we don't know the subSectionTotal points, so...
               // just use the amount scored??
               subSectionTotal = subSectionPoints;
               // add points to total and add to summary
               total += subSectionTotal;
               points += subSectionTotal;
               String s = String.format("\tSection Sub-Total: %.1f / %.1f\t\tTOTAL: %.1f / %.1f", subSectionPoints,
                     subSectionTotal, points, total);
               System.out.println(s);
               s = summary.remove(summary.size() - 1);
               s = String.format("\t%.1f / %.1f\tTOTAL: %.1f / %.1f\t%s", subSectionPoints, subSectionTotal,
                     points, total, s);
               summary.add(s);
            
               // System.out.println("Resuming after timeout code");
            
               // Regardless of whether we killed the thread or not,
               // we will break the loop because we don't know where we were
               // in reading the file.
               break;
            }
            if (input.equalsIgnoreCase("// subtotal") && summary != null) {
               // output the current sub total of points so far
               String s = String.format("\tSection Sub-Total: %.1f / %.1f\t\tTOTAL: %.1f / %.1f", subSectionPoints,
                     subSectionTotal, points, total);
               System.out.println(s);
               s = summary.remove(summary.size() - 1);
               s = String.format("\t%.1f / %.1f\tTOTAL: %.1f / %.1f\t%s", subSectionPoints, subSectionTotal,
                     points, total, s);
               summary.add(s);
               subSectionPoints = 0;
               subSectionTotal = 0;
               continue;
            } else if (input.startsWith("//") && summary != null) {
               // this line is a comment. Output the comment to the output stream.
               String s = "\t" + input.substring(3);
               summary.add(s);
               System.out.println("\n" + s);
               continue;
            } else if (input.startsWith("//")) {
               // no summary array list, just ignore and continue
               continue;
            }
            if (!file.hasNextLine()) {
               System.out.println("ERROR in test file. Expected integer for count of lines.");
               return total;
            }
         
            // to correctly parse this file, we need to read a line at a time.
            String lineAndPts = file.nextLine();
            String[] lineSplit = lineAndPts.split(" ");
            int answerCount = Integer.parseInt(lineSplit[0]);
         
            // any answer of zero length is not worth any points
            // because there is no validation that can be done
            double pointsWorth = answerCount > 0 ? 1 : 0;
            if (lineSplit.length > 1) {
               pointsWorth = Double.parseDouble(lineSplit[1]);
            }
            String[] expected = new String[answerCount];
            for (int line = 0; line < answerCount; line++) {
               if (!file.hasNextLine()) {
                  System.out.println("ERROR in test file. Unexpected end of file.");
                  return total;
               }
               expected[line] = file.nextLine();
            }
            total += pointsWorth;
            subSectionTotal += pointsWorth;
         
            System.out.print("Running Test [" + input + "]");
            String actualFull = cmdHandler.processCommand(input);
         
            if (answerCount == 0 || multiLineMatch(actualFull, expected)) {
               System.out.printf(" passed  (+%.1f pts)\n", pointsWorth);
               if (answerCount == 0) {
                  // output the result, just so we can see it.
                  System.out.println("[ " + actualFull + " ]");
               }
               points += pointsWorth;
               subSectionPoints += pointsWorth;
            } else if (breakOnFail && answerCount != 0) {
               // we have a failure. Just break out!
               System.out.println("Set to break on fail");
               return total;
            }
         
            // TODO: catch Throwable or something that catches StackOverflow
            // TODO: Assure that all exceptions are properly caught
            // TODO: Default to stopping at first failure!
         } catch (StackOverflowError e) {
            System.out.println(" Failed with Stack Overflow.");
            if (breakOnFail) {
               return total;
            }
         } catch (IllegalStateException e) {
            // likely our timeout thread was terminated
            // and our Scanner was closed.
            // Just exit and do nothing.
            return 0;
         } catch (Exception e) {
            System.out.println(" Failed with exception. Here are details:");
            e.printStackTrace();
            System.out.println(e.getMessage());
            // we have an exception. Just break out!
            if (breakOnFail) {
               return total;
            }
         }
      }
   
      if (summary != null) {
         String s = String.format("SCORE: %.1f / %.1f\n", points, total);
         if (summary.size() == 0) {
            System.out.print(s);
         }
         summary.add(s);
      }
   
      return total;
   }

   private static double startTimeout(Scanner file, int timeout, boolean breakOnFail, CommandHandler cmdHandler) {
   
      // System.out.println("Starting thread with a timeout = " + timeout);
      double score = 0;
      ExecutorService executor = Executors.newSingleThreadExecutor();
      Future<Double> future = executor.submit(
         () -> {
            return runTests(file, breakOnFail, null, cmdHandler);
         });
   
      try {
         // System.out.println("Started..");
         // this get() is a blocking call that waits for the thread to finish
         // or until the timeout hits.
         score = future.get(timeout, TimeUnit.MILLISECONDS);
         // System.out.println("Finished!");
      } catch (TimeoutException | ExecutionException | InterruptedException e) {
         future.cancel(true);
      }
   
      executor.shutdownNow();
   
      return score;
   }

   private static boolean multiLineMatch(String actualFull, String[] expected) {
      String[] actual = actualFull.split("\\n");
   
      for (int iexpected = 0; iexpected < expected.length; iexpected++) {
      
         // check for insufficient output in actual
         if (iexpected == actual.length) {
            System.out.printf(" failed: expected more output. After %d lines\n\tExpected: \"%s\"\n", iexpected,
                  expected[iexpected]);
            return false;
         }
      
         if (!actual[iexpected].equalsIgnoreCase(expected[iexpected])) {
            System.out.printf(" failure after %d lines:\n\texpected: \"%s\"\n\t  Actual: \"%s\"\n", iexpected,
                  expected[iexpected], actual[iexpected]);
            return false;
         }
      }
   
      if (actual.length > expected.length) {
         System.out.printf(" failed: actual output was too long. %d lines okay.\n\tUnexpected: \"%s\"\n",
               expected.length, actual[expected.length]);
         return false;
      }
   
      // All lines matched. Passed!
      return true;
   }
}