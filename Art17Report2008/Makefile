CLASSPATH=../java/GenerateRDF:/usr/share/java/mysql-connector-java.jar

extract: ../java/GenerateRDF/GenerateRDF.class rdfexport.properties
	rm -f *.rdf.gz *.rdf
	java -cp $(CLASSPATH) GenerateRDF >art17report2008.rdf
	gzip art17report2008.rdf
	scp *.rdf.gz root@koala:/var/local/rdfdata/htdocs/rdf

%.rdf.gz: %.rdf
	gzip $<

%.rdf: rdfexport.properties ../java/GenerateRDF/GenerateRDF.class
	java -cp $(CLASSPATH) GenerateRDF $* > $@


