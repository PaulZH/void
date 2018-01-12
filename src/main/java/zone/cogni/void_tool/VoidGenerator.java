package zone.cogni.void_tool;


import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class VoidGenerator implements Supplier<Model> {

  private static final Logger log = LoggerFactory.getLogger(VoidGenerator.class);

  private static final String VOID = "http://rdfs.org/ns/void#";

  private static List<String> voidProperties = Arrays.asList("triples", "entities",
                                                             "classes", "properties",
                                                             "distinctSubjects", "distinctObjects");

  private static String readQuery(String resource) {
    try {
      return IOUtils.toString(new ClassPathResource(resource).getInputStream(), "UTF-8");
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private final int timeout;
  private final String sparqlEndpoint;
  private final String datasetUri;
  private final String uriSpace;
  private final boolean useGraphs;
  private final String settings;

  private final Model model = ModelFactory.createDefaultModel();
  private final Map<String, Map<String, RDFNode>> typeVoidData = new HashMap<>();
  private final Map<String, Map<String, RDFNode>> propertyVoidData = new HashMap<>();


  public VoidGenerator(int timeout, String sparqlEndpoint, String datasetUri, String uriSpace, boolean useGraphs, String settings) {
    this.timeout = timeout;
    this.sparqlEndpoint = sparqlEndpoint;
    this.datasetUri = datasetUri;
    this.uriSpace = uriSpace;
    this.useGraphs = useGraphs;
    this.settings =  settings;

    model.setNsPrefix("void", VOID);
  }

  @Override public Model get() {
    addSettingsAsComment(); // basic tool documentation

    addSparqlEndpoint(); // 3.2
    addExampleResources(); // 4.1
    addUriSpace(); // 4.2
    addVocabularies(); // 4.3

    getGlobalStatistics(); // 4.6
    getClassStatistics(); // 4.5
    getPropertyStatistics(); // 4.5

    return model;
  }

  private void addSettingsAsComment() {
    model.add(ResourceFactory.createStatement(ResourceFactory.createResource(datasetUri),
                                              RDFS.comment,
                                              ResourceFactory.createPlainLiteral(settings)));

  }

  private void addVocabularies() {
    Arrays.asList("4.3/vocabularyClasses.sparql", "4.3/vocabularyProperties.sparql")
            .forEach(this::addVocabularies);
  }

  private void addVocabularies(String name) {
    RunQuery.runFromResource(timeout, sparqlEndpoint, useGraphs, name).ifPresent(queryResult -> {
      Set<String> vocabularies = queryResult.getRows().stream()
              .map(row -> row.get("result").asResource().getURI())
              .map(uri -> uri.contains("#") ? StringUtils.substringBeforeLast(uri, "#")
                                            : StringUtils.substringBeforeLast(uri, "/") + "/")
              .collect(Collectors.toSet());

      vocabularies.forEach(vocabulary -> {
        model.add(ResourceFactory.createStatement(ResourceFactory.createResource(datasetUri),
                                                  ResourceFactory.createProperty(VOID, "vocabulary"),
                                                  ResourceFactory.createResource(vocabulary)));
      });
    });
  }

  private void addSparqlEndpoint() {
    model.add(ResourceFactory.createStatement(ResourceFactory.createResource(datasetUri),
                                              ResourceFactory.createProperty(VOID, "sparqlEndpoint"),
                                              ResourceFactory.createResource(sparqlEndpoint)));

  }

  private void getGlobalStatistics() {

    voidProperties.forEach(voidProperty -> addDatasetStatement(voidProperty, getTotal("4.6/" + voidProperty + ".sparql")));
  }

  private void addDatasetStatement(String property, RDFNode value) {
    if (value == null) return;

    model.add(ResourceFactory.createStatement(ResourceFactory.createResource(datasetUri),
                                              ResourceFactory.createProperty(VOID, property),
                                              value));
  }

  private void getClassStatistics() {
    voidProperties.forEach(voidProperty -> fillTypeVoidData(voidProperty,
                                                            runQuery("4.5/class/" + voidProperty + ".sparql")));

    typeVoidData.forEach((type, typeVoid) -> {
      Resource classResource = ResourceFactory.createResource();
      typeVoid.forEach((voidProperty, rdfNode) -> {

        model.add(ResourceFactory.createStatement(ResourceFactory.createResource(datasetUri),
                                                  ResourceFactory.createProperty(VOID, "classPartition"),
                                                  classResource));

        model.add(ResourceFactory.createStatement(classResource,
                                                  ResourceFactory.createProperty(VOID, voidProperty),
                                                  rdfNode));

        model.add(ResourceFactory.createStatement(classResource,
                                                  ResourceFactory.createProperty(VOID, "class"),
                                                  ResourceFactory.createResource(type)));

      });
    });
  }

  private void fillTypeVoidData(String property, Optional<QueryResult> queryResultOptional) {
    queryResultOptional.ifPresent(
            queryResult ->
                    queryResult.getRows().forEach(row -> {
                      RDFNode type = row.get("type");
                      String typeUri = type.asResource().getURI();

                      if (!typeVoidData.containsKey(typeUri)) {
                        typeVoidData.put(typeUri, new HashMap<>());
                      }

                      Map<String, RDFNode> voidData = typeVoidData.get(typeUri);
                      voidData.put(property, row.get("total"));
                    })
    );
  }

  private void getPropertyStatistics() {
    voidProperties.forEach(voidProperty -> fillPropertyVoidData(voidProperty,
                                                                runQuery("4.5/property/" + voidProperty + ".sparql")));

    propertyVoidData.forEach((property, propertyVoid) -> {
      Resource propertyResource = ResourceFactory.createResource();
      propertyVoid.forEach((voidProperty, rdfNode) -> {

        model.add(ResourceFactory.createStatement(ResourceFactory.createResource(datasetUri),
                                                  ResourceFactory.createProperty(VOID, "propertyPartition"),
                                                  propertyResource));

        model.add(ResourceFactory.createStatement(propertyResource,
                                                  ResourceFactory.createProperty(VOID, voidProperty),
                                                  rdfNode));

        model.add(ResourceFactory.createStatement(propertyResource,
                                                  ResourceFactory.createProperty(VOID, "property"),
                                                  ResourceFactory.createResource(property)));

      });
    });
  }

  private void fillPropertyVoidData(String property, Optional<QueryResult> queryResultOptional) {
    queryResultOptional.ifPresent(queryResult -> {
      queryResult.getRows().forEach(row -> {
        RDFNode propertyNode = row.get("property");
        String propertyUri = propertyNode.asResource().getURI();

        if (!propertyVoidData.containsKey(propertyUri)) {
          propertyVoidData.put(propertyUri, new HashMap<>());
        }

        Map<String, RDFNode> voidData = propertyVoidData.get(propertyUri);
        voidData.put(property, row.get("total"));
      });
    });
  }

  private void addExampleResources() {
    String sparqlName = (useGraphs ? "quads" : "triples") +  "/4.1/exampleResources.sparql";
    String uriSpaceFilter = StringUtils.isBlank(uriSpace) ? ""
                                                          : "\n && (STRSTARTS(STR(?s), '" + uriSpace + "') )";
    String sparql = MessageFormatter.format(readQuery(sparqlName), uriSpaceFilter).getMessage();
//    log.debug("SPARQL: {}", sparql);
    RunQuery.runFromSparql(timeout, sparqlEndpoint, sparql, sparqlName).ifPresent(
            queryResult -> {
              List<Resource> uris = queryResult.getRows().stream()
                      .map(row -> row.get("example").asResource())
                      .collect(Collectors.toList());

              uris.forEach(uri -> {
                model.add(ResourceFactory.createStatement(ResourceFactory.createResource(datasetUri),
                                                          ResourceFactory.createProperty(VOID, "exampleResource"),
                                                          uri));
              });
            }
    );
  }

  private void addUriSpace() {
    if (StringUtils.isNotBlank(uriSpace)) {
      model.add(ResourceFactory.createStatement(ResourceFactory.createResource(datasetUri),
                                                ResourceFactory.createProperty(VOID, "uriSpace"),
                                                ResourceFactory.createPlainLiteral(uriSpace)));
    }
  }

  private Optional<QueryResult> runQuery(String resource) {
    return RunQuery.runFromResource(timeout, sparqlEndpoint, useGraphs, resource);
  }

  private RDFNode getTotal(String resource) {
    QueryResult queryResult = runQuery(resource).orElse(null);

    if (queryResult == null) return null;
    return queryResult.getRows().get(0).get("total");
  }

}
