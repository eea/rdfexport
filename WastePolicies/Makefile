CLASSPATH=../java/GenerateRDF:/usr/share/java/mysql-connector-java.jar

extract: ../java/GenerateRDF/GenerateRDF.class rdfexport.properties
	rm -f *.rdf.gz *.rdf
	java -cp $(CLASSPATH) GenerateRDF >wastepolicies.rdf
#	gzip wastepolicies.rdf
#	scp *.rdf.gz root@koala:/var/local/rdfdata/htdocs/

%.rdf.gz: %.rdf
	gzip $<

%.rdf: rdfexport.properties ../java/GenerateRDF/GenerateRDF.class
	java -cp $(CLASSPATH) GenerateRDF $* > $@


