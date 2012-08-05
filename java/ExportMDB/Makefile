CLASSPATH=.:lib/Access_JDBC40.jar:/usr/share/java/junit.jar
test: compile
	java -cp $(CLASSPATH) ExportMDB

compile: ExportMDB.class

ExportMDB.class: ExportMDB.java
	javac -cp $(CLASSPATH) ExportMDB.java

run: compile
	java -cp $(CLASSPATH) ExportMDB

