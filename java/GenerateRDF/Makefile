CLASSPATH=.:/usr/share/java/mysql-connector-java.jar:/usr/share/java/junit.jar
test: compile
	java -cp $(CLASSPATH) GenerateRDFTest

compile: GenerateRDF.class GenerateRDFTest.class

GenerateRDF.class: GenerateRDF.java
	javac -cp $(CLASSPATH) GenerateRDF.java

GenerateRDFTest.class: GenerateRDFTest.java
	javac -cp $(CLASSPATH) GenerateRDFTest.java

run: compile
	java -cp $(CLASSPATH) GenerateRDF -i 1367 species

