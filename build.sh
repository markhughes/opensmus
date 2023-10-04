rm -rf ./dist

mvn clean
mvn compile
mvn package

mkdir -p dist/dependencies
mkdir -p dist/scripts

cp target/OpenSMUS*.jar dist/OpenSMUS.jar
cp distrib/Movie.cfg dist
cp distrib/OpenSMUS.cfg dist
cp distrib/Scriptmap.cfg dist
cp distrib/start.sh dist


cp distrib/dependencies/hsqldb/hsqldb/*/*.jar dist/dependencies/hsqldb.jar
cp distrib/dependencies/org/jboss/netty/netty/*/*.jar dist/dependencies/netty.jar
