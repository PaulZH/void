package zone.cogni.void_tool;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.exit;
import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.API.run;
import static javaslang.Predicates.is;

public class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);

  private static String usage =
          "usage: void.jar       [--datasetUri uri] \n" +
                  "                      [--sparqlEndpoint url] \n" +
                  "optional:             [--file      : where output is written, default dataset.ttl]\n" +
                  "                      [--useGraphs : true or false, default false. Use true if you want to query all graphs  instead of only the default graph] \n" +
                  "                      [--format    : default TURTLE  \n" +
                  "                                     one of RDF/XML, RDF/XML-ABBREV, N-TRIPLE, TURTLE and N3] \n" +
                  "                      [--timeout   : query timeout in seconds, default 300]\n" +
                  "                      [--uriSpace  : uri space of dataset, also used to limit example resources]";


  private static String datasetUri;
  private static String sparqlEndpoint;
  private static int timeoutInSeconds = 300;
  private static String format = "TURTLE";
  private static String file = "dataset.ttl";
  private static String uriSpace;
  private static boolean useGraphs;

  public static void main(String[] args) throws IOException {

    long start = currentTimeMillis();

    processArguments(Arrays.asList(args));

    VoidGenerator voidGenerator = new VoidGenerator(timeoutInSeconds, sparqlEndpoint, datasetUri, uriSpace, useGraphs, getSettings());
    Model model = voidGenerator.get();

    model.write(new FileOutputStream(file), format);
    log.info("{} triples written to file '{}'.", model.size(), file);
    log.info("Total time {}s.", (currentTimeMillis() - start) / 1000);
  }

  private static void processArguments(List<String> arguments) {
    if (arguments.isEmpty() || arguments.get(0).equals("--help")) {
      System.out.println(usage);
      exit(0);
    }

    for (int i = 0; i < arguments.size() - 1; i += 2) {

      String argument = arguments.get(i);
      String value = arguments.get(i + 1);


      Match(argument).of(
              Case(is("--datasetUri"), () -> datasetUri = value),
              Case(is("--sparqlEndpoint"), () -> sparqlEndpoint = value),
              Case(is("--useGraphs"), () -> useGraphs = Boolean.parseBoolean(value)),
              Case(is("--format"), () -> format = value),
              Case(is("--timeout"), () -> timeoutInSeconds = Integer.parseInt(value)),
              Case(is("--file"), () -> file = value),
              Case(is("--uriSpace"), () -> uriSpace = value),
              Case(is("--help"), () -> run(Main::giveHelp)),
              Case($(), () -> run(() -> invalidArgument(argument)))
      );
    }

    if (uriSpace == null) uriSpace = "";

    printSettings();

    checkArguments();
  }

  private static void checkArguments() {
    boolean fail = false;

    if (StringUtils.isBlank(datasetUri)) {
      System.out.println("Invalid parameters: --datasetUri is not set.");
      fail = true;
    }

    if (StringUtils.isBlank(sparqlEndpoint)) {
      System.out.println("Invalid parameters: --sparqlEndpoint is not set.");
      fail = true;
    }

    if (fail) giveHelp();
  }

  private static void printSettings() {
    System.out.println(getSettings());
  }

  private static String getSettings() {
    return "\n" +
            "\n" +
            "Running with settings: " + "\n" +
            "\n" +
            "\t\t Dataset uri     : " + datasetUri + "\n" +
            "\t\t Use graphs      : " + useGraphs + "\n" +
            "\t\t Uri space       : " + uriSpace + "\n" +
            "\t\t Sparql endpoint : " + sparqlEndpoint + "\n" +
            "\t\t Timeout         : " + timeoutInSeconds + "\n" +
            "\t\t File            : " + file + "\n" +
            "\t\t Format          : " + format + "\n" +
            "\n";
  }

  private static void invalidArgument(String argument) {
    System.err.println("invalid argument given: '" + argument + "'");
    giveHelp();
  }

  private static void giveHelp() {
    System.out.println(usage);
    exit(1);
  }

}
