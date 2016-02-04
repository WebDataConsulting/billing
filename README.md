jBilling
========

**Requirements:**

* Java 8+
* Grails 2.4.3
* PostgreSQL 8+ *(required for testing, other runtime databases are supported)*

To run jBilling from source you will need to have Java 8+ and Grails 2.4.3 installed. To install grails, download version 2.4.3 from the [Grails Archive](http://www.grails.org/download/) and follow the installation instructions for your operating system. 

[Java](http://www.oracle.com/technetwork/java/javase/downloads/index.html) and [PostgreSQL](http://www.postgresql.org/) can be downloaded an installed by visiting the vendor's websites, or in linux environments by using the package manager (apt-get or yum).



Cloning the Source Code from GitHub
-----------------------------------

Install [Git](http://git-scm.com/download/). 

Clone the repository from `git@github.com:WebDataConsulting/billing.git` 

<pre><code>git clone git@github.com:WebDataConsulting/billing.git jB-CE-4.1.1</code></pre>


Configuring PostgreSQL
----------------------

To run jBilling with the out-of-box reference database, you must have PostgreSQL installed and configured with a **'jbilling'** user and an empty **'jbilling_test'** database. The setup scripts also expect that the user will allow local connections without a password.

Edit the PostgreSQL pg_hba.conf file and change the "local" and "IPv4" localhost connection types:

<pre><code># "local" is for Unix domain socket connections only
local   all         all                               trust
# IPv4 local connections:
host    all         all         127.0.0.1/32          trust
</code></pre>

Explanation: Set up PostgreSQL to allow all local connections to the database without using a password


Restart the PostgreSQL server

<pre><code>sudo service postgresql restart
</code></pre>


Connect to PostgreSQL and create the test user and database.

<pre><code>sudo -u postgres psql
</code></pre>


Connect to PostgreSQL and create the test user and database.

<pre><code>CREATE ROLE jbilling WITH LOGIN SUPERUSER CREATEDB CREATEROLE PASSWORD 'jbilling';
CREATE DATABASE jbilling_test WITH OWNER jbilling;
</code></pre>



Setup
-----

JAVA_HOME -> /opt/lib/jdk1.8.0_60 or an equivalent.

GRAILS_HOME -> /opt/lib/grails-2.4.3 or equivalent.

Java and grails executables should be in your classpath.



Copy the jbilling properties file from the src directory to the root of the project

<pre><code>cp ./src/java/jbilling.properties .</code></pre>


Run the grails `compile` target to compile the jBilling source code, then run the `prepare-test` target to load the reference database and prepare all the required resources.

<pre><code>
grails compile
grails prepare-test
</pre></code>

*The grails compile target may halt with a compiler error on some environments, running compile a second time usually resolves the issue.*

Running from Source
-------------------

<pre><code>**Windows**: ``run-app.bat``</pre></code>

<pre><code>**Linux/Mac**: ``./run-app.sh``</pre></code>


Logging in
----------

Browse to


<pre><code>
http://localhost:8080
</pre></code>


Select "Prancing Pony" from "Child company" 
Credentials:

user: admin
password: 123qwe


Setup with an empty database
----------------------------

<pre><code>grails compile
grails prepare-test --init
</pre></code>


If you want to use a database that is not the standard one:

<pre><code>grails prepare-test -init -user=<username> -db=<database_name> -pass=<password-for-dbuser> -url=<dburl> -driver=<driver-full-class-name>
</code></pre>


