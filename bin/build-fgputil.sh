#!/usr/bin/env sh

set -e

docker=$1
dir=$(pwd)

echo "Cloning latest FgpUtil version"

git clone \
  --depth 1 \
  --quiet \
  https://github.com/VEuPathDB/FgpUtil
cd FgpUtil

echo "Building FgpUtil"

mvn clean install 2>&1 | grep '^\[[A-Z]'

if [ "${docker}" = "docker" ]; then
  cp Util/target/fgputil-util-1.0.0.jar "${dir}/fgputil-util-1.0.0.jar"
  cp AccountDB/target/fgputil-accountdb-1.0.0.jar "${dir}/fgputil-accountdb-1.0.0.jar"
else
  mkdir -p "${dir}/vendor"
  cp Util/target/fgputil-util-1.0.0.jar "${dir}/vendor/fgputil-util-1.0.0.jar"
  cp AccountDB/target/fgputil-accountdb-1.0.0.jar "${dir}/vendor/fgputil-accountdb-1.0.0.jar"
fi

cd "${dir}"
rm -rf FgpUtil
