How to use:

* Compile the Java code. See README.txt at the top
* Copy the database.properties-dist to database.properties and change the values
* Run the command:

java -jar ../../target/rdf-exporter-1.0-SNAPSHOT-jar-with-dependencies.jar \
   -d database.properties \
   -f rdfexport.properties \
   -z -o art17report2014.rdf.gz

If you want to discover a new database you can do:

java -jar ../../target/rdf-exporter-1.0-SNAPSHOT-jar-with-dependencies.jar \
   -d database.properties \
   -xp
