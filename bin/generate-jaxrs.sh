#!/usr/bin/env sh

. bin/lib/colors.sh

set -eu

APP_PACKAGE=${1:?Missing required application package param}

compile() {
  java -jar bin/raml-to-jaxrs.jar ./api.raml \
    --directory src/main/java \
    --generate-types-with jackson \
    --model-package ${APP_PACKAGE}.generated.model \
    --resource-package ${APP_PACKAGE}.generated.resources \
    --support-package ${APP_PACKAGE}.generated.support 2>&1
}

echo "${C_BLUE}Generating JaxRS Java Code${C_NONE}"

out="$(compile)"

echo "${out}" | sed 's/^/  /'
