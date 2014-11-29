RDFExport
=========

1. Introduction
---------------

This README explains how to use the RDF exporter JAR that
you can build with Maven by issuing the following command
in the root directory of this project:

```
$ mvn clean install
```
or
```
$ mvn -Dmaven.test.skip=true clean install
```

The JAR is generated into the target/ directory auto-created
by Maven. It will be named rdf-exporter-xx.jar, where the
'xx' is the version number or version label stated in pom.xml.

2. What this JAR does and how to use it
---------------------------------------

The JAR is capable of generating RDF out of a given relational
database, using the JDBC protocol. Depending on command-line
options, it can automatically discover all the tables, columns,
primary and foreign keys by itself, or you can provide the
tables-to-export and queries-to-run through a properties
file that we call below as "RDF export properties file".

The output file is specified with the -o option. If it's not given,
standard output is used.

For the auto-discovery, provide -x as command line option. If you
supply -xa, the auto-discovery mode will prompt you for confirmation
on all tables and foreign keys discovered.

Auto-discovered information will be saved into a given output
file and no RDF exported, when you provide -p command line option.

The database connection properties and also numerous properties required
for the RDF generation are given in a properties file whose path is
supplied via the -f command line option.

If the database is an MS-Access file (aka MDB file) or a dBase (aka DBF)
directory, then it can be provided with the -T command line option.
Alternatively, it can be provided through the full JDBC connection URL
in database connection properties or via the db.templateFilePath property.

Note that the DBF driver needs a directory name, and it then looks for DBF
files in it. Those are then seen as tables. You therefore specify the folder-
not the file.

Naturally, the JDBC driver must be on the classpath.

3. The properties file
----------------------

Queries are stored in a Java properties file. The full description is
provided in the docs/FILEFORMAT.html

4. Execution and command line options
-------------------------------------

The usage of rdf-exporter-xx.jar is as follows:

```
$ java -cp target/rdf-exporter-1.0-SNAPSHOT.jar eionet.rdfexport.Execute <options>
```
  or:
```
$ java -jar target/rdf-exporter-1.0-SNAPSHOT-jar-with-dependencies.jar <options>
```

If `<options>` is not supplied, then a help text on possible options is printed:

----
Usage: This command accepts the following command line arguments:

```
 -f input_properties_file    Path of the input properties file containing everything needed for RDF generation. That includes the database's JDBC url, JDBC driver class name, datatype mappings, namespaces, SQL queries to export, etc.
 -o rdf_output_file          Path of the RDF output file to be generated.
 -T template_properties_file From this file and auto-discovered info about the database, the output_properties_file is generated that can then be used as an input_properties_file for multiple reuse.
 -J jdbc_database_url        The URL to the database.
 -D jdbc_driver_class        For MySQL use com.mysql.jdbc.Driver.
 -U database_user            The user to log into the database.
 -P password                 The password for the database.
 -p                          Generate a properties file from auto-discovered info. If -T and -p have been specified, then -f is ignored and no RDF output generated. Instead, the output_properties_file will be generated and the program exits.
 -z                          The RDF output file will be zipped. if this argument is present.
 -m                          Path of the MS Access file to query from. Overrides the one given in input_properties_file or template_properties_file.
 -l                          List tables in the database.
 -x                          Tables/keys of the database will be auto-discovered.
 -xc                         Tables/keys will be auto-discovered, user prompted for confirmation.
 -B base_uri                 Base URI which overrides the one in the input_properties_file or template_properties_file.
 -V vocabulary_uri           Vocabulary URI which overrides the one in the input_properties_file or template_properties_file.
 -i rowId                    Only records with this primary key value will be exported.
 -h or -?                    Show this help
```
Unrecognized arguments will be treated as names of tables to export. If no arguments are found, all tables will be exported.
