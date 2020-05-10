#!/usr/bin/env sh

echo "Cloning raml-for-jax-rs v3.0.5"
git clone \
  --branch 3.0.5 \
  --depth 1 \
  --quiet \
  https://github.com/mulesoft-labs/raml-for-jax-rs.git tmp/raml || exit 1

curDir=$(pwd)

cd tmp/raml/raml-to-jaxrs/raml-to-jaxrs-cli || exit 1
echo "Correcting pom.xml"
sed -i 's/3.0.5-SNAPSHOT/3.0.5/' pom.xml

echo "Running maven build"
(mvn clean install || exit 1) | grep -v "Downloading\|already added, skipping\|Downloaded"
mv target/raml-to-jaxrs-cli-3.0.5-jar-with-dependencies.jar "${curDir}"/bin/raml-to-jaxrs.jar
cd "${curDir}"
rm -rf tmp
