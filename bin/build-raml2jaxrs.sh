#!/usr/bin/env sh

set -eu

echo "Cloning raml-for-jax-rs"
git clone \
  --branch 3.0.5 \
  --depth 1 \
  https://github.com/mulesoft-labs/raml-for-jax-rs.git tmp/raml > /dev/null 2>&1

curDir=$(pwd)

cd tmp/raml/raml-to-jaxrs/raml-to-jaxrs-cli
sed -i 's/3.0.5-SNAPSHOT/3.0.5/' pom.xml

echo "Running maven build"
mvn clean install | grep -v "Downloading\|already added, skipping\|Downloaded"
mv target/raml-to-jaxrs-cli-3.0.5-jar-with-dependencies.jar "${curDir}"/raml-to-jaxrs.jar
cd "${curDir}"
rm -rf tmp
