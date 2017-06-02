


This tool allows to generation void data for a sparql endpoint.

It covers following sections of the Void specification.
* 3.2 SPARQL endpoints (void:sparqlEndpoint)
* 4.1 Example resources (void:exampleResource )
* 4.2 Patterns for resource URIs (void:uriSpace)
* 4.3 Vocabularies used in a dataset (void:vocabulary)
* 4.5 Partitioning a dataset based on classes and properties
* 4.6 Providing statistics about datasets

For more information see https://www.w3.org/TR/void/.

## Using the tool

From the commandline output

```
usage: void.jar [--datasetUri uri] [--sparqlEndpoint url] 
optional:             [--file : where output is written, default dataset.ttl]
                      [--format  : default TURTLE  
                                   one of RDF/XML, RDF/XML-ABBREV, N-TRIPLE, TURTLE and N3] 
                      [--timeout : query timeout in seconds, default 300]
                      [--uriSpace : uri space of dataset]
```

Example                     

```
java -jar void-1.0.0.jar 
   --sparqlEndpoint http://data.kbodata.be/sparql    
   --datasetUri http://data.kbodata.be/dataset/kbo#id           --file kbo.ttl

```
                      
## Building

On Windows
                      
```
gradlew.bat build
```

On Mac or Unix

```
./gradlew build
```

