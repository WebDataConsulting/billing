grails clean --offline

grails compile --offline

grails compile --offline

grails jar 

grails copy-resources --offline

grails compile-reports --offline

grails compile-designs --offline

dropdb jbilling_test -U jbilling
 
createdb jbilling_test -U jbilling

grails --offline prepare-test --verbose

echo "REMOVING EXISTING FILES localDB.*"

rm -rf image/bin/hsql/localDB*

echo "REMOVED...NOW CLEANING"

grails clean --offline

echo "CLEANED...NOW COMPILING"

grails compile --offline

grails compile --offline

echo "COMPILED AND CONNECTED WITH DB...:) :)"

grails package-public-release -update --offline

echo "GENERATING SCRIPTS NOW..."

liquibase-3.2.3/liquibase --classpath=lib/hsqldb-2.3.2.jar --url=jdbc:hsqldb:file:image/bin/hsql/localDB --username=sa --password= --driver=org.hsqldb.jdbcDriver --changeLogFile=descriptors/database/jbilling-schema.xml --contexts=base update

liquibase-3.2.3/liquibase --classpath=lib/hsqldb-2.3.2.jar --url=jdbc:hsqldb:file:image/bin/hsql/localDB --username=sa --password= --driver=org.hsqldb.jdbcDriver --changeLogFile=descriptors/database/jbilling-init_data.xml update

liquibase-3.2.3/liquibase --classpath=lib/hsqldb-2.3.2.jar --url=jdbc:hsqldb:file:image/bin/hsql/localDB --username=sa --password= --driver=org.hsqldb.jdbcDriver --changeLogFile=descriptors/database/jbilling-schema.xml --contexts=FKs update

liquibase-3.2.3/liquibase --classpath=lib/hsqldb-2.3.2.jar --url=jdbc:hsqldb:file:image/bin/hsql/localDB --username=sa --password= --driver=org.hsqldb.jdbcDriver --changeLogFile=descriptors/database/jbilling-upgrade-3.2.xml --contexts=base update

liquibase-3.2.3/liquibase --classpath=lib/hsqldb-2.3.2.jar --url=jdbc:hsqldb:file:image/bin/hsql/localDB --username=sa --password= --driver=org.hsqldb.jdbcDriver --changeLogFile=descriptors/database/jbilling-upgrade-3.3.xml --contexts=base update

liquibase-3.2.3/liquibase --classpath=lib/hsqldb-2.3.2.jar:target/classes/ --url=jdbc:hsqldb:file:image/bin/hsql/localDB --username=sa --password= --driver=org.hsqldb.jdbcDriver --changeLogFile=descriptors/database/jbilling-upgrade-3.4.xml --contexts=base update

liquibase-3.2.3/liquibase --classpath=lib/hsqldb-2.3.2.jar --url=jdbc:hsqldb:file:image/bin/hsql/localDB --username=sa --password= --driver=org.hsqldb.jdbcDriver --changeLogFile=descriptors/database/jbilling-upgrade-4.0.xml --contexts=base update

liquibase-3.2.3/liquibase --classpath=lib/hsqldb-2.3.2.jar:target/classes/ --url=jdbc:hsqldb:file:image/bin/hsql/localDB --username=sa --password= --driver=org.hsqldb.jdbcDriver --changeLogFile=descriptors/database/jbilling-upgrade-4.1.xml --contexts=base update

echo "SCRIPTS GENERATED SUCCESSFULLY..."

java -cp lib/hsqldb-2.3.2.jar org.hsqldb.util.DatabaseManager

echo "NOW CHANGING TABLES..."

perl -pi -e 's/"filter"/FILTER/' image/bin/hsql/localDB.script

perl -pi -e 's/"language"/LANGUAGE/' image/bin/hsql/localDB.script

perl -pi -e 's/"role"/ROLE/' image/bin/hsql/localDB.script

perl -pi -e 's/"value"/VALUE/' image/bin/hsql/localDB.script

perl -pi -e 's/"source"/SOURCE/' image/bin/hsql/localDB.script

perl -pi -e 's/"global"/GLOBAL/' image/bin/hsql/localDB.script

perl -pi -e 's/"until"/UNTIL/' image/bin/hsql/localDB.script

perl -pi -e 's/"min"/"MIN"/' image/bin/hsql/localDB.script

perl -pi -e 's/"max"/"MAX"/' image/bin/hsql/localDB.script

echo "SCRIPT READY...NOW PACKAGING STARTS..."

grails package-public-release --offline

echo "ZIP READY..."

cd target

unzip -q jbilling-community-4.1.1.zip

cd jbilling-community-4.1.1/bin

chmod +x *.sh

./startup.sh && tail -f ../logs/catalina.out
