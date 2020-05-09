#!/usr/bin/env sh

. bin/lib/colors.sh
echo "${C_BLUE}Checking for Raml to JaxRS${C_NONE}"

if ! stat bin/raml-to-jaxrs.jar > /dev/null 2>&1; then
  echo "${C_CYAN}  Not found.  Installing.${C_NONE}"
  bin/build-raml2jaxrs.sh 2>&1 | sed 's/^/  /'
fi
