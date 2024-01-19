Year 2 Advanced Programming Hackathon Assignment

# Introduction

There are many earthquakes each year and around the world, even though not all of them cause
significant danger or widespread property damage. There are a number of scientific institutions
worldwide who monitor seismic activity to pinpoint the source and magnitude of earthquakes,
including both the British [1] and US Geological Survey [2] organisations (BGS and USGS,
respectively). The USGS publish data on all known earthquakes and other significant seismic activity
[3], enabling researchers to monitor trends and verify findings.

Using the USGS data, I have created a SQLite database of all the earthquakes worldwide since the
year 1900 with a magnitude of at least 5.0 on the Richter scale. I omitted earthquakes that were
smaller smaller or older to limit the size of the database. Your task is to create a web service that
allows users to retrieve data about the earthquakes in the database.

You will create your web service using the Spark Java [4] microservice framework, the SQLite JDBC
driver [5], the reference JSON parser [6], and Java’s built-in DOM XML parser [7], as you used in the
lab sessions for Advanced Programming. **DO NOT** use any other libraries on this assessment: code
written using other libraries will not be marked. You should download the starter project, in which
the database, libraries, and a small amount of starter code are already set-up, from Moodle.

[SPECIFICATION](Hackathon Assessment Specification.pdf)
