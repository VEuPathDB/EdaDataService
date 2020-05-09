#!/usr/bin/env sh

# Don't run this in docker
[ "${DOCKER}" = "build" ] && exit 0

. bin/lib/colors.sh
readonly LIBS="ojdbc8.jar
  ucp.jar
  xstreams.jar"

echo "${C_BLUE}Checking for Oracle JDBC libraries${C_NONE}"

if ! stat $(echo "${LIBS}" | sed 's#^ *#vendor/#') > /dev/null 2>&1; then
  echo "${C_CYAN}  Not found.  Installing${C_NONE}"
  wget -q https://download.oracle.com/otn_software/linux/instantclient/19600/instantclient-basiclite-linux.x64-19.6.0.0.0dbru.zip
  mkdir -p vendor
  unzip -j instantclient-basiclite-linux.x64-19.6.0.0.0dbru.zip \
        $(echo "${LIBS}" | sed 's#^ *#instantclient_19_6/#') -d vendor
  rm instantclient-basiclite-linux.x64-19.6.0.0.0dbru.zip
fi
