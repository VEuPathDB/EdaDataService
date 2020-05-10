#!/usr/bin/env sh

if [ "${DOCKER:-local}" != "build" ]; then
  readonly C_BLUE="\033[94m"
  readonly C_CYAN="\033[36m"
  readonly C_RED="\033[31m"
  readonly C_YELLOW="\033[33m"
  readonly C_NONE="\033[0m"
else
  readonly C_BLUE=""
  readonly C_CYAN=""
  readonly C_RED=""
  readonly C_YELLOW=""
  readonly C_NONE=""
fi
