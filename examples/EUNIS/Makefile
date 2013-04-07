CLASSPATH=../java/GenerateRDF:/usr/share/java/mysql-connector-java.jar


extract: habitatdistribution.rdf.gz speciesconservation.rdf.gz
	scp habitatdistribution.rdf.gz speciesconservation.rdf.gz root@koala:/var/local/rdfdata/htdocs/eunis/

speciesconservation.rdf.gz: speciesconservation.properties ../java/GenerateRDF/GenerateRDF.class
	java -cp $(CLASSPATH) GenerateRDF -f speciesconservation.properties |zcat >speciesconservation.rdf.gz

habitatdistribution.rdf.gz: habitatdist.properties ../java/GenerateRDF/GenerateRDF.class
	java -cp $(CLASSPATH) GenerateRDF -f habitatdist.properties |zcat >habitatdistribution.rdf.gz

%.rdf.gz: %.rdf
	gzip $<

%.rdf: rdfexport.properties ../java/GenerateRDF/GenerateRDF.class
	java -cp $(CLASSPATH) GenerateRDF $* > $@


