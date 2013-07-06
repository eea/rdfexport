/**
Export RDF from a database. Queries are stored in a properties file.
There are two types of queries. A plain select and an attributes
table. For the plain select the class will use the first column as the
<em>identifier</em>, and create RDF properties for the
other columns.

<h2>Syntax of the properties file</h2>

The prefix for all rdf:abouts and rdf:resources to make them into URLs. Follows xml:base rules.
It is recommended to end the string with '/'
<pre>
baseurl = http://your-webapp.com/resource/
</pre>

A 'vocabulary' entry entry is mandatory - it specifies, which default prefix
should be used for vocabulary elements such as classes and properties.
<pre>
vocabulary = http://your-webapp.com/vocabulary/
</pre>

You must specify what other namespace prefixes you use in the queries like this:
<pre>
xmlns.rdf = http://www.w3.org/1999/02/22-rdf-syntax-ns#
xmlns.rdfs = http://www.w3.org/2000/01/rdf-schema#
xmlns.dcterms = http://purl.org/dc/terms/
xmlns.geo = http://www.w3.org/2003/01/geo/wgs84_pos#
</pre>

For each Java simple type you can specify what RDF data type to map to.
<pre>
datatype.real = xsd:decimal
</pre>

You can provide a number of queries. Each query, however, should
select information about an object of a pseudo-table. This pseudo-table is used to
construct corresponding URIs for the objects returned by the query.
The pseudo table does not need to have the same name as the table you query.
You can have several queries for the same pseudo-table. Just append a number
or letter to the "query" key, as in emissions.query1, emissions.query2 ...

The first column returned by the query represents the ID of the object and
has to be named "id", all other columns represent characteristics (or
properties of this object). As column identifier you should reuse existing
vocabularies whenever possible. If your "user" table, for example, contains a
column named "first_name" this can be easily mapped to the corresponding FOAF
property using: "SELECT id,first_name AS 'foaf:firstName' FROM user".

You can use the following column naming convention in order to inform
RDFGenerator about the datatype or language of a column:
 SELECT id, price AS 'price^^xsd:decimal', desc AS 'rdf:label@en' FROM products
RDFGenerator tries to autodetect datatypes and convert appropriately.

For the attributes table the result must have one + X * four columns: 1. id, 2. attribute name, 3. value, 4. datatype, 5.
languagecode, 6. attribute name, 7. value, 8. datatype, 9. languagecode, etc.

Some of the columns of the queries will contain references to other
objects rather than literal values. The following configuration
specifies which columns are references to other objects
It can also be specified directly in the query.
The right hand side can be a pseudo-table name or a full URL.
<pre>
objectproperty.forCountry = http://rdfdata.eionet.europa.eu/eea/countries
objectproperty.hasNotation = notations
</pre>

The &lt;pseudo-table&gt;.class sets the rdf:type. If not specified, the capitalized table name is used.
<pre>
emissions.class = Emission
</pre>

A query or attribute table without pseudo-table is considered metadata for the RDF document, and
will usually contain license and provenance information.

<h2>Examples:</h2>

<h3>Simple query</h3>
<pre>
taxonomy.query = SELECT ID_TAXONOMY AS id, \
        ID_TAXONOMY AS 'code@', \
        level, name, ID_TAXONOMY_LINK AS 'link-&gt;taxonomy', \
        ID_TAXONOMY_PARENT AS 'parent-&gt;taxonomy', \
        notes, \
        ID_DC AS 'dcterms:source-&gt;references' \
        FROM T_TAXONOMY
</pre>

<h3>Pseudo table without a real table behind</h3>
<pre>
notations.class = rdf:Description
notations.attributetable1 = SELECT 'IE' AS id \
 ,'rdf:type','http://rdfdata.eionet.europa.eu/lrtap/ontology/Notation','-&gt;',NULL \
 ,'rdfs:label','Included elsewhere','','' \
 ,'skos:notation','IE','','' \
 ,'skos:prefLabel','Included elsewhere','',''

notations.attributetable2 = SELECT 'NA' AS id \
 ,'rdf:type','http://rdfdata.eionet.europa.eu/lrtap/ontology/Notation','-&gt;',NULL \
 ,'rdfs:label','Not applicable','','' \
 ,'skos:notation','NA','','' \
 ,'skos:prefLabel','Not applicable','',''
</pre>
 */
package eionet.rdfexport;
