APP_PACKAGE  := $(shell ./gradlew -q print-package)
SCHEMA_FILES := $(shell find docs/schema -name '*.json' -o -name '*.yml' -o -name '*.yaml')
PWD          := $(shell pwd)
MAIN_DIR     := src/main/java/$(shell echo $(APP_PACKAGE) | sed 's/\./\//g')
GEN_DIR      := $(MAIN_DIR)/generated
GEN_FILES    := $(shell find $(GEN_DIR) -name '*.java')
DOC_FILES    := src/main/resources/api.html docs/api.html
ALL_PACKABLE := $(shell find src/main -type f)


default:
	@echo $(GEN_DIR)
	@echo "Please choose one of:"
	@echo ""
	@echo "  make compile"
	@echo "    Compiles the existing code in 'src/'.  Regenerates files if the"
	@echo "    api spec has changed."
	@echo ""
	@echo "  make test"
	@echo "    Compiles the existing code in 'src/' and runs unit tests."
	@echo "    Regenerates files if the api spec has changed."
	@echo ""
	@echo "  make jar"
	@echo "    Compiles a 'fat jar' from this project and it's dependencies."
	@echo ""
	@echo "  make docker"
	@echo "    Builds a runnable docker image for this service"
	@echo ""
	@echo "  make install-r2j"
	@echo "    Builds the raml-to-jaxrs.jar file used for generating java code"
	@echo "    from raml."
	@echo ""
	@echo "  make install-dev-env"
	@echo "    Ensures the current dev environment has the necessary "
	@echo "    installable tools to build this project."
	@echo ""
	@echo "  make install-fgputil:"
	@echo "    Ensures that a build fgputil jar is available as a dependency."
	@echo ""

.PHONY: compile
compile: $(GEN_FILES) $(DOC_FILES)
	@./gradlew clean compileJava

.PHONY: test
test: $(GEN_FILES) $(DOC_FILES)
	@./gradlew clean test

.PHONY: jar
jar: install-gradle install-fgputil build/libs/service.jar

.PHONY: docker
docker:
	@docker build -t $(shell ./gradlew -q print-container-name) .

.PHONY: cleanup-example
cleanup-example:
	@echo "Removing demo code"
	@rm -rf "$(GEN_DIR)" \
		"$(MAIN_DIR)/service/*"

.PHONY: install-r2j
install-r2j:
	@bin/build-raml2jaxrs.sh

.PHONY: install-dev-env
install-dev-env: install-fgputil build-r2j
	@bin/prepare-env.sh

.PHONY: install-fgputil
install-fgputil:
	@echo "Ensuring FgpUtil is available"
	@stat vendor/fgputil-accountdb-1.0.0.jar > /dev/null 2>&1 || bin/install-fgputil.sh
	@stat vendor/fgputil-util-1.0.0.jar > /dev/null 2>&1 || bin/install-fgputil.sh

.PHONY: install-gradle
install-gradle:
	@echo "Ensuring Gradle is available"
	@stat .gradle > /dev/null 2>&1 \
		|| (echo "Downloading Gradle" && ./gradlew tasks > /dev/null)

#
# File based targets
#

build/libs/service.jar: $(ALL_PACKABLE) vendor/fgputil-accountdb-1.0.0.jar vendor/fgputil-util-1.0.0.jar build.gradle.kts service.properties
	@echo "Building application jar"
	@./gradlew clean test jar


$(GEN_FILES): api.raml docs/raml/library.raml
	@echo "Generating JaxRS Java Code"
	@java -jar raml-to-jaxrs.jar ./api.raml \
		--directory src/main/java \
		--generate-types-with jackson \
		--model-package $(APP_PACKAGE).generated.model \
		--resource-package $(APP_PACKAGE).generated.resources \
		--support-package $(APP_PACKAGE).generated.support

$(DOC_FILES): api.raml docs/raml/library.raml
	@echo "Generating API Documentation"
	@raml2html api.raml > docs/api.html
	@cp docs/api.html src/main/resources/api.html

docs/raml/library.raml: $(SCHEMA_FILES)
	@echo "Converting JSON Schema to Raml"
	@bin/schema2raml.sh
