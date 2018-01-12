
## Introduction

Using this tool you can generate VoID data for a SPARQL endpoint.

It covers following sections of the VoID specification.
* 3.2 SPARQL endpoints (void:sparqlEndpoint)
* 4.1 Example resources (void:exampleResource )
* 4.2 Patterns for resource URIs (void:uriSpace)
* 4.3 Vocabularies used in a dataset (void:vocabulary)
* 4.5 Partitioning a dataset based on classes and properties
* 4.6 Providing statistics about datasets
  * void:triples
  * void:entities
  * void:classes
  * void:properties
  * void:distinctSubjects
  * void:distinctObjects

List of properties of section 4.6 is also valid for 4.5.

For more information see https://www.w3.org/TR/void/.

## Using the tool

From the commandline output

```
usage: void.jar       [--datasetUri uri] 
                      [--sparqlEndpoint url] 
optional:             [--file      : where output is written, default dataset.ttl]
                      [--useGraphs : true or false, default false. Use true if you want to query all graphs instead of only the default graph] 
                      [--format    : default TURTLE  
                                     one of RDF/XML, RDF/XML-ABBREV, N-TRIPLE, TURTLE and N3] 
                      [--timeout   : query timeout in seconds, default 300]
                      [--uriSpace  : uri space of dataset, also used to limit example resources]
```


Example                     

```
java -jar void.jar 
   --sparqlEndpoint http://data.kbodata.be/sparql    
   --datasetUri http://data.kbodata.be/dataset/kbo#id           
   --file kbo.ttl
   --uriSpace http://data.kbodata.be
```

will start and show

```
Running with settings: 

		 Dataset uri     : http://data.kbodata.be/dataset/kbo#id
		 Use graphs      : false
		 Uri space       : http://data.kbodata.be
		 Sparql endpoint : http://data.kbodata.be/sparql
		 Timeout         : 1200
		 File            : kbo.ttl
		 Format          : TURTLE
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

