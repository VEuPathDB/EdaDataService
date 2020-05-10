APP_PACKAGE  := $(shell ./gradlew -q print-package)
SCHEMA_FILES := $(shell find docs/schema -name '*.raml')
PWD          := $(shell pwd)
MAIN_DIR     := src/main/java/$(shell echo $(APP_PACKAGE) | sed 's/\./\//g')
GEN_DIR      := $(MAIN_DIR)/generated
GEN_FILES    := $(shell find $(GEN_DIR) -name '*.java')
DOC_FILES    := src/main/resources/api.html docs/api.html
ALL_PACKABLE := $(shell find src/main -type f)

C_BLUE := "\\033[94m"
C_NONE := "\\033[0m"
C_CYAN := "\\033[36m"

.PHONY: default
default:
	@echo "Please choose one of:"
	@echo ""
	@echo "$(C_BLUE)  make compile$(C_NONE)"
	@echo "    Compiles the existing code in 'src/'.  Regenerates files if the"
	@echo "    api spec has changed."
	@echo ""
	@echo "$(C_BLUE)  make test$(C_NONE)"
	@echo "    Compiles the existing code in 'src/' and runs unit tests."
	@echo "    Regenerates files if the api spec has changed."
	@echo ""
	@echo "$(C_BLUE)  make jar$(C_NONE)"
	@echo "    Compiles a 'fat jar' from this project and it's dependencies."
	@echo ""
	@echo "$(C_BLUE)  make docker$(C_NONE)"
	@echo "    Builds a runnable docker image for this service"
	@echo ""
	@echo "$(C_BLUE)  make install-dev-env$(C_NONE)"
	@echo "    Ensures the current dev environment has the necessary "
	@echo "    installable tools to build this project."
	@echo ""

.PHONY: compile
compile: install-dev-env $(GEN_FILES) $(DOC_FILES)
	@./gradlew clean compileJava

.PHONY: test
test: install-dev-env $(GEN_FILES) $(DOC_FILES)
	@./gradlew clean test

.PHONY: jar
jar: install-dev-env build/libs/service.jar

.PHONY: docker
docker:
	@docker build -t $(shell ./gradlew -q print-container-name) .

.PHONY: cleanup-example
cleanup-example:
	@echo "$(C_BLUE)Removing demo code$(C_NONE)"
	@find "$(GEN_DIR)" -type d -delete
	@rm -rf "$(MAIN_DIR)/service/*"

.PHONY: install-dev-env
install-dev-env:
	@bin/check-env.sh
	@bin/install-fgputil.sh
	@bin/install-oracle.sh
	@bin/install-raml2jaxrs.sh
	@bin/install-npm.sh

#
# File based targets
#

build/libs/service.jar: $(ALL_PACKABLE) vendor/fgputil-accountdb-1.0.0.jar vendor/fgputil-util-1.0.0.jar build.gradle.kts service.properties
	@echo "$(C_BLUE)Building application jar$(C_NONE)"
	@./gradlew clean test jar


$(GEN_FILES): api.raml docs/raml/library.raml
	@bin/generate-jaxrs.sh $(APP_PACKAGE)

$(DOC_FILES): api.raml docs/raml/library.raml
	@echo "$(C_BLUE)Generating API Documentation$(C_NONE)"
	@raml2html api.raml > docs/api.html
	@cp docs/api.html src/main/resources/api.html

docs/raml/library.raml: $(SCHEMA_FILES)
	@echo "$(C_BLUE)Converting JSON Schema to Raml$(C_NONE)"
	@bin/merge-raml.sh
