How to use rdf-exporter-xx.jar
==============================
Usage: java -cp rdf-export-xx.jar eionet.rdfexport.Execute {mode} {flags}
If mode is 'rdf', generate rdf mode is selected and the following flags are in use:
[-o output_file] [-f rdf_properties_file] [-d database_properties_file] [-i identifier_to_export] [-z]
Flags:   -o file name of the generated RDF in file (console output when not specified)
         -f rdf export properties file (rdfexport.properties when not specified)
         -d database connection properties (database.properties when not specified)
         -i only export the record with the identifier, all other arguments are expected to be table names
         -z gzip the output
If mode is 'db', export database in rdf mode is selected and the following flags are in use:
[-p output_file] [-f template_properties_file] [-d database_properties_file] [-m database_file]
Flags:   -p save the discovered information as a properties file
         -f load the template properties from the specified file
         -d load the database properties from the specified file
         -m the name of the database file to investigate

How to use ExportDB (mode=db)
=============================
The ExportDB can investigate any database that adheres the
JDBC standard.

For MDB:
Put you MDB file in the current folder.
The database access configuration is in mdb.properties. It is configured to
look in the current directory. You can modify it to hardwire the MDB database
or you can provide the database using the -m argument

 java -cp rdf-exporter-xx.jar;Access_JDBC40.jar eionet.rdfexport.Execute db -d mdb.properties -m MyDatabase.mdb

For DBF:
Put you DBF file in the testdbf folder. The DBF driver needs a directory name,
and it then looks for DBF files in it. Those are then seen as tables. You
therefore specify the folder - not the file.

The database access configuration is in dbf.properties. It is configured to
look in the current directory. To query the test data, do:

 java -cp rdf-exporter-xx.jar;Access_JDBC40.jar eionet.rdfexport.Execute db -d dbf.properties -m testdbf


How to use with GenerateRDF (mode=rdf)
======================================
ExportMDB can generate a properties file that can be tweaked, and then parsed
by GenerateRDF. The database parameters are also written into the properties
file, and therefore you must supply the file twice:

 java -cp rdf-exporter-xx.jar;Access_JDBC40.jar db -d mdb.properties -p redlist.properties -m European_Red_List_September2011.mdb
 java -cp rdf-exporter-xx.jar;Access_JDBC40.jar eionet.rdfexport.Execute rdf -z -d redlist.properties -f redlist.properties  >European_Red_List_September2011.rdf.gz
 java -cp rdf-exporter-xx.jar;mysql-connector-java-5.1.6.jar eionet.rdfexport.Execute rdf
