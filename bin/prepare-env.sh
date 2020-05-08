#!/usr/bin/env sh

skip=0
if [ "$1" = "docker" ]; then
  skip=1
fi

readonly PREFIX_WARN="[WARN] "
readonly PREFIX_ERROR="[ERROR]"

command -v docker > /dev/null
if [ $? -gt 0 ] && [ $skip -eq 0 ]; then
  echo "${PREFIX_WARN} Docker is not installed, some make commands will not work."
fi

command -v mvn > /dev/null
if [ $? -gt 0 ] && [ $skip -eq 0 ]; then
  echo "${PREFIX_ERROR} Maven is required.  Please install it and try again"
  exit 1
fi

command -v npm > /dev/null
if [ $? -gt 0 ]; then
  echo "${PREFIX_ERROR} NodeJS is required.  Please install it and try again."
  exit 1
fi

npm install -g \
  json-dereference-cli \
  raml2html \
  ramldt2jsonschema
