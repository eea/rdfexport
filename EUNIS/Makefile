CLASSPATH=../java/GenerateRDF:/usr/share/java/mysql-connector-java.jar

run: ../GenerateRDF/GenerateRDF.class



extract: ../java/GenerateRDF/GenerateRDF.class rdfexport.properties habitatdistribution.rdf.gz
	scp habitatdistribution.rdf.gz root@koala:/var/local/rdfdata/eunis/

birdlife.rdf: birdlife.properties ../java/GenerateRDF/GenerateRDF.class
	java -cp $(CLASSPATH) GenerateRDF -f birdlife.properties >birdlife.rdf

habitatdistribution.rdf: habitatdist.properties ../java/GenerateRDF/GenerateRDF.class
	java -cp $(CLASSPATH) GenerateRDF -f habitatdist.properties >habitatdistribution.rdf

%.rdf.gz: %.rdf
	gzip $<

%.rdf: rdfexport.properties ../java/GenerateRDF/GenerateRDF.class
	java -cp $(CLASSPATH) GenerateRDF $* > $@


