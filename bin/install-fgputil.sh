#!/usr/bin/env sh

set -e

docker=$1

dir=$(pwd)

echo "Cloning FgpUtil"
git clone https://github.com/VEuPathDB/FgpUtil > /dev/null
cd FgpUtil/Util

echo "Building FgpUtil"
mvn clean install 2>&1 | grep '^\[[A-Z]'

if [ "${docker}" = "docker" ]; then
  cp target/fgputil-util-1.0.0.jar "${dir}/fgputil-util-1.0.0.jar"
else
  mkdir -p "${dir}/vendor"
  cp target/fgputil-util-1.0.0.jar "${dir}/vendor/fgputil-util-1.0.0.jar"
fi

cd "${dir}"
rm -rf FgpUtil
