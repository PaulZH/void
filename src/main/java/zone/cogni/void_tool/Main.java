package zone.cogni.void_tool;

import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.exit;

@SuppressWarnings({"UseOfSystemOutOrSystemErr", "CallToSystemExit"})
@SpringBootApplication
public class Main implements CommandLineRunner {

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

  public static void main(String[] args) {
    SpringApplication.run(Main.class, args);
  }

  @Override
  public void run(String... args) throws FileNotFoundException {
    long start = currentTimeMillis();

    processArguments(Arrays.asList(args));

    VoidGenerator voidGenerator = new VoidGenerator(timeoutInSeconds, sparqlEndpoint, datasetUri, uriSpace, useGraphs, getSettings());
    Model model = voidGenerator.get();

    model.write(new FileOutputStream(file), format);
    log.info("{} triples written to file '{}'.", model.size(), file);
    log.info("Total time {}s.", (currentTimeMillis() - start) / 1000);
  }

  private  void processArguments(List<String> arguments) {
    if (arguments.isEmpty() || arguments.get(0).equals("--help")) {
      System.out.println(usage);
      exit(0);
    }

    for (int i = 0; i < arguments.size() - 1; i += 2) {
      String argument = arguments.get(i);
      String value = arguments.get(i + 1);


      Match(argument).of(
              Case($("--datasetUri"), () -> datasetUri = value),
              Case($("--sparqlEndpoint"), () -> sparqlEndpoint = value),
              Case($("--useGraphs"), () -> useGraphs = Boolean.parseBoolean(value)),
              Case($("--format"), () -> format = value),
              Case($("--timeout"), () -> timeoutInSeconds = Integer.parseInt(value)),
              Case($("--file"), () -> file = value),
              Case($("--uriSpace"), () -> uriSpace = value),
              Case($("--help"), () -> Try.run(Main::giveHelp)),
              Case($(), () -> Try.run(() -> invalidArgument(argument)))
      );
    }

    if (uriSpace == null) uriSpace = "";

    printSettings();

    checkArguments();
  }

  private  void checkArguments() {
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

  private  void printSettings() {
    System.out.println(getSettings());
  }

  private  String getSettings() {
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

  private  void invalidArgument(String argument) {
    System.err.println("invalid argument given: '" + argument + "'");
    giveHelp();
  }

  private static void giveHelp() {
    System.out.println(usage);
    exit(1);
  }


}
