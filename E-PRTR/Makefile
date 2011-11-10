CLASSPATH=../java/GenerateRDF:../java/GenerateRDF/jtds-1.2.5.jar

extract: ../java/GenerateRDF/GenerateRDF.class rdfexport.properties
	rm -f *.rdf.gz *.rdf
	java -cp $(CLASSPATH) GenerateRDF contry medium pollutant pollutantthreshold wastethreshold wastetype unit method methodtype rbd >lookuptables.rdf
	java -cp $(CLASSPATH) GenerateRDF facilityreport >facilityreport.rdf
	java -cp $(CLASSPATH) GenerateRDF pollutantrelease >pollutantrelease.rdf
	java -cp $(CLASSPATH) GenerateRDF pollutanttransfer >pollutanttransfer.rdf
	java -cp $(CLASSPATH) GenerateRDF wastetransfer >wastetransfer.rdf
	gzip lookuptables.rdf pollutantrelease.rdf facilityreport.rdf pollutanttransfer.rdf wastetransfer.rdf
	scp *.rdf.gz root@koala:/var/local/rdfdata/htdocs/rdf/eprtr

test: ../java/GenerateRDF/GenerateRDF.class
	java -cp $(CLASSPATH) GenerateRDF -i WATER medium

individual: \
contry.rdf.gz \
facilityreport.rdf.gz \
medium.rdf.gz \
method.rdf.gz \
methodtype.rdf.gz \
pollutant.rdf.gz \
pollutantrelease.rdf.gz \
pollutantthreshold.rdf.gz \
pollutanttransfer.rdf.gz \
rbd.rdf.gz \
unit.rdf.gz \
wastethreshold.rdf.gz \
wastetransfer.rdf.gz \
wastetype.rdf.gz


%.rdf.gz: %.rdf
	gzip $<

%.rdf: rdfexport.properties ../java/GenerateRDF/GenerateRDF.class
	java -cp $(CLASSPATH) GenerateRDF $* > $@


