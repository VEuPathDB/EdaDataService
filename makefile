APP_PACKAGE  := $(shell ./gradlew -q print-package)
PWD          := $(shell pwd)
MAIN_DIR     := src/main/java/$(shell echo $(APP_PACKAGE) | sed 's/\./\//g')
TEST_DIR     := $(shell echo $(MAIN_DIR) | sed 's/main/test/')
GEN_DIR      := $(MAIN_DIR)/generated
ALL_PACKABLE := $(shell find src/main -type f)

EXAMPLE_DIR      := src/main/java/org/veupathdb/service/demo
EXAMPLE_TEST_DIR := src/test/java/org/veupathdb/service/demo

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
compile: install-dev-env gen-jaxrs gen-docs
	@./gradlew clean compileJava

.PHONY: test
test: install-dev-env gen-jaxrs gen-docs
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

fix-path:
ifneq "$(MAIN_DIR)" "$(EXAMPLE_DIR)"
	@mkdir -p $(MAIN_DIR)
	@mv -t $(MAIN_DIR) $(EXAMPLE_DIR)/*
	@mv -t $(TEST_DIR) $(EXAMPLE_TEST_DIR)/*
	@rm -rf $(EXAMPLE_DIR) $(EXAMPLE_TEST_DIR)
endif

#
# File based targets
#

build/libs/service.jar: gen-jaxrs gen-docs vendor/fgputil-accountdb-1.0.0.jar vendor/fgputil-util-1.0.0.jar build.gradle.kts service.properties
	@echo "$(C_BLUE)Building application jar$(C_NONE)"
	@./gradlew clean test jar


gen-jaxrs:
	@bin/generate-jaxrs.sh $(APP_PACKAGE)

gen-docs: api.raml docs/raml/library.raml
	@echo "$(C_BLUE)Generating API Documentation$(C_NONE)"
	@raml2html docs/raml/full-api.raml > docs/api.html
	@cp docs/api.html src/main/resources/api.html

docs/raml/library.raml: $(SCHEMA_FILES)
	@echo "$(C_BLUE)Converting JSON Schema to Raml$(C_NONE)"
	@bin/merge-raml.sh
