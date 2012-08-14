How to use ExportMDB
====================

For MDB:

Install the Access_JDBC40.jar in lib/Access_JDBC40.jar.

Put you MDB file in the current folder.
The database access configuration is in mdb.properties. It is configured to
look in the current directory. You can modify it to hardwire the MDB database
or you can provide the database using the -m argument



For DBF:
Install the DBF_JDBC40.jar in lib/DBF_JDBC40.jar.

Put you DBF file in the current folder.
The DBF driver needs a directory name, and it then looks for DBF files in it
Those are then seen as tables. You therefore specify the folder, not the file.

The database access configuration is in dbf.properties. It is configured to
look in the current directory. To query the test data, do:

 java -cp .:lib/DBF_JDBC40.jar ExportMDB -d dbf.properties -m testdbf


