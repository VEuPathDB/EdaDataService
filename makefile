SCHEMA_FILES := $(shell find docs/schema -name '*.json')
PWD          := $(shell pwd)

default:
	@echo "Please choose one of:"
	@echo ""
	@echo "  make compile"
	@echo "    Compiles the existing code in 'src/'."
	@echo ""
	@echo "  make gen-compile"
	@echo "    Generates docs & JaxRS then compiles."
	@echo ""
	@echo "  make test"
	@echo "    Compiles the existing code in 'src/' and runs unit tests."
	@echo ""
	@echo "  make build-jar"
	@echo "    - Generates docs and JaxRS types from Raml"
	@echo "    - Compiles and tests the java code."
	@echo "    - Packages a runnable fat jar"
	@echo ""
	@echo "  make build-docker"
	@echo "    Builds a docker image for this service"
	@echo ""
	@echo "  make build-r2j"
	@echo "    Builds the raml-to-jaxrs.jar file used for generating java code from raml."
	@echo ""
	@echo "  make prep-env"
	@echo "    Ensures the current dev environment has the necessary installable tools to"
	@echo "    build this project."
	@echo ""
	@echo "  install-fgputil:"
	@echo "    Ensures that a build fgputil jar is available as a dependency."
	@echo ""
	@echo "  make gen-jaxrs"
	@echo "    Generates JaxRS types from Raml.  As a prerequisite, the compiled"
	@echo "    raml-to-jaxrs.jar must be in the workspace."
	@echo ""
	@echo "  make gen-docs"
	@echo "    Generates api docs from Raml, places the generated docs in both the"
	@echo "    src/main/resources/api.html and docs/api.html."
	@echo ""

compile:
	@./gradlew clean compileJava

gen-compile: gen-docs gen-jaxrs compile

test:
	@./gradlew clean test

build-jar: gradle-ping gen-jaxrs gen-docs
	@echo "Building application jar"
	@./gradlew clean test jar

build-docker:
	@docker build -t $(shell ./gradlew -q print-container-name) .

local-dev: prep-env install-fgputil build-r2j

prep-env:
	@bin/prepare-env.sh

build-r2j:
	@bin/build-raml2jaxrs.sh

gen-jaxrs : APP_PACKAGE = $(shell ./gradlew -q print-package)
gen-jaxrs: docs/raml/library.raml
	@echo "Generating JaxRS Java Code"
	@java -jar raml-to-jaxrs.jar ./api.raml \
		--directory src/main/java \
		--generate-types-with jackson \
		--model-package $(APP_PACKAGE).model \
		--resource-package $(APP_PACKAGE).resources \
		--support-package $(APP_PACKAGE).support 2>&1

gen-docs: docs/raml/library.raml
	@echo "Generating API Documentation"
	@raml2html api.raml > docs/api.html
	@cp docs/api.html src/main/resources/api.html

docs/raml/library.raml: $(SCHEMA_FILES)
	@echo "Converting JSON Schema to Raml"
	@bin/schema2raml.sh

install-fgputil:
	@echo "Ensuring FgpUtil is available"
	@stat vendor/fgputil > /dev/null 2>&1 || bin/install-fgputil.sh

cleanup-example : APP_PACKAGE = $(shell ./gradlew -q print-package | sed 's/\./\//g')
cleanup-example:
	@rm -rf "src/main/java/$(APP_PACKAGE)/model" \
		"src/main/java/$(APP_PACKAGE)/resources" \
		"src/main/java/$(APP_PACKAGE)/support" \
		"src/main/java/$(APP_PACKAGE)/service/*"

# Makes sure the gradle image is downloaded
gradle-ping:
	@stat .gradle > /dev/null 2>&1 \
		|| (echo "Downloading Gradle" && ./gradlew tasks > /dev/null)
