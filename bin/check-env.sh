#!/usr/bin/env sh

. bin/lib/colors.sh
readonly ERROR="${C_RED}[ERROR]${C_NONE} "
readonly  WARN="${C_YELLOW}[WARN ]${C_NONE} "
OK=true

echo "${C_BLUE}Checking development environment${C_NONE}"

if ! command -v npm > /dev/null; then
  echo "  ${ERROR}NPM is not installed.  Please install it and try again or build in Docker."
  OK=false
fi

if ! command -v mvn > /dev/null; then
  echo "  ${ERROR}Maven is not installed.  Please install it and try again or build in Docker."
  OK=false
fi

if ! command -v docker > /dev/null; then
  echo "  ${WARN}Docker is not installed.  The command 'make docker' will not work."
fi

if [ "${OK}" = "false" ]; then
  exit 1
fi
