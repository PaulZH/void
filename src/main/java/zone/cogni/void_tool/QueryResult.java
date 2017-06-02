package zone.cogni.void_tool;

import org.apache.jena.rdf.model.RDFNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class QueryResult {

  private final Set<String> variables;
  private final List<Map<String, RDFNode>> rows;

  public QueryResult(Set<String> variables, List<Map<String, RDFNode>> rows) {
    this.variables = variables;
    this.rows = rows;
  }

  public Set<String> getVariables() {
    return variables;
  }

  public List<Map<String, RDFNode>> getRows() {
    return rows;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder("QueryResult{");
    result.append("variables=").append(variables);
    result.append(", rows=\n");
    rows.forEach(row -> result.append("\t\t").append(row).append("\n"));
    result.append('}');
    return result.toString();
  }
}
