
************************************************************
1. Introduction
************************************************************

This README explains how to use the RDF exporter JAR that
you can build with Maven by issuing the following command
in the root directory of this project:

> mvn clean install

The JAR is generated into the target/ directory auto-created
by Maven. It will be named rdf-exporter-xx.jar, where the
'xx' is the version number or version label stated in pom.xml.

************************************************************
2. What this JAR does and how to use it.
************************************************************

The JAR is capable of generating RDF out of a given relational
database, using the JDBC protocol. Depending on command-line
options, it can automatically discover all the tables, columns,
primary and foreign keys by itself, or you can provide the
tables-to-export and queries-to-run through a properties
file that we call below as "RDF export properties file".

For the auto-discovery, provide -x as command line option. If you
supply -xa, the auto-discovery mode will prompt you for confirmation
on all tables and foreign keys discovered.

Auto-discovered information will be saved into a given properties
file and no RDF exported, when you provide that file's path in the
-p command line option.

The database connection properties are provided by a properties
file that we call below as "database connection properties file".

The RDF output file is specified with the -o option. If it's not given,
standard output is used.

If the database is a MS-Access file (aka MDB file) or a dBase (aka DBF)
directory, then it can be provided with the -m command line option.
Aletrnatively, it can be provided through the full JDBC connection URL
in database connection properties file.

Note that the DBF driver needs a directory name, and it then looks for DBF
files in it. Those are then seen as tables. You therefore specify the folder-
not the file.

Naturally, the JDBC driver must be on the classpath.

************************************************************
3. Execution and command line options.
************************************************************

The usage of rdf-exporter-xx.jar is as follows:

> java -cp ./target/rdf-export-xx.jar eionet.rdfexport.Execute <options>

If <options> is not supplied, then a help text on possible options is printed:

-d database_properties_file   Path to the database connection properties file.
-f rdf_properties_file        Path to the RDF export properties file.
-o rdf_output_file            Path to the RDF output file.
-m ms_access_file             Path to the MS-Access database file or DBF to export.
-z                            The RDF output file will be zipped.
-x                            Tables/keys will be auto-discovered.
-xa                           Tables/keys will be auto-discovered, user prompted for confirmation.
-p properties_output_file     If -x or -xa given then auto-discovered info is saved into this file.
-i rowId                      Only records with this primary key value will be exported.

NB! Note that unrecognized arguments will be treated as names of tables to export. If not
such arguments are found, all tables will be exported. Alternatively, you can use -xa option
to be prompted for confirmation on each table (i.e. interactive mode).

