package zone.cogni.void_tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;

public class RunQuery implements Supplier<Optional<QueryResult>> {

  private static final Logger log = LoggerFactory.getLogger(RunQuery.class);

  public static Optional<QueryResult> runFromResource(int timeout, String sparqlEndpoint, boolean useGraphs, String queryResource) {
    queryResource = (useGraphs ? "quads" : "triples") + "/" + queryResource;
    return runFromSparql(timeout, sparqlEndpoint, readQuery(queryResource), queryResource);
  }

  public static Optional<QueryResult> runFromSparql(int timeout, String sparqlEndpoint, String sparql, String sparqlName) {
    return new RunQuery(timeout, sparqlEndpoint, sparql, sparqlName).get();
  }

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
  private final String sparql;
  private final String sparqlName;

  private RunQuery(int timeout, String sparqlEndpoint, String sparql, String sparqlName) {
    this.timeout = timeout;
    this.sparqlEndpoint = sparqlEndpoint;
    this.sparql = sparql;
    this.sparqlName = sparqlName;
  }

  @Override public Optional<QueryResult> get() {
    try {
      String bodyAsString = getRequestResultXmlAsString();
//      log.info(bodyAsString);
      if (bodyAsString == null) return Optional.empty();

      JsonNode jsonNode = convertXmlToJson(bodyAsString);
      if (jsonNode.size() == 0) {
        log.info("Query failed.");
        log.debug("Query returned \n{}", bodyAsString.substring(0, Math.min(1024, bodyAsString.length())));
        return Optional.empty();
      }

      return Optional.of(new QueryResult(getVariables(jsonNode), getResults(jsonNode)));
    }
    catch (Exception e) {
      log.warn("Failed query '{}'. {}", sparqlName, e.getMessage());
      log.debug("Stacktrace", e);
      return Optional.empty();
    }
  }

  private JsonNode convertXmlToJson(String bodyAsString) {
    try {
      JSONObject jObject = XML.toJSONObject(bodyAsString);
      ObjectMapper xmlToJsonMapper = new ObjectMapper();
      xmlToJsonMapper.enable(SerializationFeature.INDENT_OUTPUT);

      Object json = xmlToJsonMapper.readValue(jObject.toString(), Object.class);
      String output = xmlToJsonMapper.writeValueAsString(json);

      ObjectMapper treeMapper = new ObjectMapper();
      return treeMapper.readTree(output);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private CloseableHttpClient getHttpClient() {
    int connectionRequestTimeout = timeout * 1000;
    RequestConfig requestConfig = RequestConfig.custom().
            setConnectionRequestTimeout(connectionRequestTimeout).setConnectTimeout(connectionRequestTimeout).setSocketTimeout(connectionRequestTimeout).build();
    return HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
  }

  private String getRequestResultXmlAsString() {
    try {
      String sparqlEncoded = UriUtils.encodeQueryParam(sparql, "UTF-8");
      HttpGet request = new HttpGet(sparqlEndpoint + "?query=" + sparqlEncoded);

      String value = "application/sparql-results+xml";
      request.addHeader(HttpHeaders.ACCEPT, value);

      CloseableHttpResponse response = getHttpClient().execute(request);
      if (200 <= response.getStatusLine().getStatusCode() && response.getStatusLine().getStatusCode() < 300) {
        log.info("Call ok for '{}'.", sparqlName);
        return EntityUtils.toString(response.getEntity());
      }

      log.error("Call failed for '{}'.", sparqlName);
      log.debug(EntityUtils.toString(response.getEntity()));
      return null;
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private List<Map<String, RDFNode>> getResults(JsonNode jsonNode) {
//    log.debug("JSON {}", jsonNode);

    JsonNode path = jsonNode.path("sparql").path("results").path("result");

    if (path instanceof ObjectNode) {
      return Collections.singletonList(getRowMap((ObjectNode) path));
    }

    assertThat(path).isInstanceOf(ArrayNode.class);

    List<Map<String, RDFNode>> result = new ArrayList<>();
    ArrayNode array = (ArrayNode) path;
    Iterator<JsonNode> resultIterator = array.elements();
    while (resultIterator.hasNext()) {
      ObjectNode resultNode = (ObjectNode) resultIterator.next();
      result.add(getRowMap(resultNode));
    }

    return result;
  }

  private Map<String, RDFNode> getRowMap(ObjectNode resultNode) {
    JsonNode bindingNode = resultNode.path("binding");
    if (bindingNode instanceof ObjectNode) {
      Map<String, RDFNode> row = new HashMap<>();
      addSingleBinding(row, (ObjectNode) bindingNode);
      return row;
    }

    Map<String, RDFNode> row = new HashMap<>();

    ArrayNode binding = (ArrayNode) bindingNode;
    Iterator<JsonNode> bindingIterator = binding.elements();
    while (bindingIterator.hasNext()) {
      addSingleBinding(row, (ObjectNode) bindingIterator.next());
    }

    return row;
  }

  private void addSingleBinding(Map<String, RDFNode> row, ObjectNode singleBinding) {
    String name = ((TextNode) singleBinding.get("name")).textValue();

    if (singleBinding.has("uri")) {
      String uri = singleBinding.get("uri").textValue();
      row.put(name, ResourceFactory.createResource(uri));
      return;
    }

    if (singleBinding.has("literal")) {
      JsonNode literalNode = singleBinding.get("literal");
      if (literalNode instanceof TextNode) {
        throw new RuntimeException("TextNode not supported: " + literalNode);
      }
      else if (literalNode instanceof ObjectNode) {
        ObjectNode literalNodeAsObject = (ObjectNode) literalNode;
        String datatype = literalNodeAsObject.get("datatype").textValue();
        String content = literalNodeAsObject.get("content").asText(); // important asText !!

        RDFDatatype typeByName = TypeMapper.getInstance().getTypeByName(datatype);
        row.put(name, ResourceFactory.createTypedLiteral(content, typeByName));
      }
    }

  }

  private Set<String> getVariables(JsonNode jsonNode) {
    JsonNode path = jsonNode.path("sparql").path("head").path("variable");

    if (path instanceof ObjectNode) {
      return Collections.singleton(getVariable((ObjectNode) path));
    }

    assertThat(path).isInstanceOf(ArrayNode.class);

    Set<String> result = new HashSet<>();

    ArrayNode array = (ArrayNode) path;
    Iterator<JsonNode> elements = array.elements();
    while (elements.hasNext()) {
      result.add(getVariable((ObjectNode) elements.next()));
    }

    return result;
  }

  private String getVariable(ObjectNode variableNode) {
    return variableNode.get("name").textValue();
  }

}
