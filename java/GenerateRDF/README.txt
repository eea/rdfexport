How to use ExportMDB
====================
The mis-named ExportMDB can investigate any database that adheres the
JDBC standard.

For MDB:

Install the Access_JDBC40.jar in lib/Access_JDBC40.jar.

Put you MDB file in the current folder.
The database access configuration is in mdb.properties. It is configured to
look in the current directory. You can modify it to hardwire the MDB database
or you can provide the database using the -m argument

 java -cp .:lib/Access_JDBC40.jar ExportMDB -d mdb.properties -m MyDatabase.mdb


For DBF:
Install the DBF_JDBC40.jar in lib/DBF_JDBC40.jar.

Put you DBF file in the testdbf folder. The DBF driver needs a directory name,
and it then looks for DBF files in it. Those are then seen as tables. You
therefore specify the folder - not the file.

The database access configuration is in dbf.properties. It is configured to
look in the current directory. To query the test data, do:

 java -cp .:lib/DBF_JDBC40.jar ExportMDB -d dbf.properties -m testdbf


How to use with GenerateRDF
---------------------------
ExportMDB can generate a properties file that can be tweaked, and then parsed
by GenerateRDF. The database parameters are also written into the properties
file, and therefore you must supply the file twice:

 java -cp .:lib/Access_JDBC40.jar ExportMDB -d mdb.properties -p redlist.properties -m European_Red_List_September2011.mdb
 java -cp .:lib/Access_JDBC40.jar GenerateRDF -z -d redlist.properties -f redlist.properties  >European_Red_List_September2011.rdf.gz
