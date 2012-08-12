CLASSPATH=.:/usr/share/java/mysql-connector-java.jar:lib/Access_JDBC40.jar:/usr/share/java/junit.jar

mdb: ExportMDB.class GenerateRDF.class
	java -cp .:lib/Access_JDBC40.jar ExportMDB -m WFD.mdb

test: compile
	java -cp $(CLASSPATH) GenerateRDFTest

compile: GenerateRDF.class GenerateRDFTest.class

GenerateRDF.class: GenerateRDF.java
	javac -cp $(CLASSPATH) GenerateRDF.java

ExportMDB.class: ExportMDB.java
	javac -cp $(CLASSPATH) ExportMDB.java

GenerateRDFTest.class: GenerateRDFTest.java
	javac -cp $(CLASSPATH) GenerateRDFTest.java

run: compile
	java -cp $(CLASSPATH) GenerateRDF -i 1367 species

