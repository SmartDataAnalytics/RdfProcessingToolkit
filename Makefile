CWD = $(shell pwd)

# Maven Clean Install Skip ; skip tests, javadoc, scaladoc, etc
MS = mvn -DskipTests -Dmaven.javadoc.skip=true -Dskip
MCIS = $(MS) clean install

# Source: https://stackoverflow.com/questions/4219255/how-do-you-get-the-list-of-targets-in-a-makefile
.PHONY: help

.ONESHELL:
help:   ## Show these help instructions
	@sed -rn 's/^([a-zA-Z_-]+):.*?## (.*)$$/"\1" "\2"/p' < $(MAKEFILE_LIST) | xargs printf "make %-20s# %s\n"

deb-rebuild: ## rebuild the deb package (minimal build of only required modules)
	$(MCIS) -Pdeb -am -pl :rdf-processing-toolkit-pkg-deb-cli $(ARGS)

deb-reinstall: ## Reinstall deb (requires prior build)
	@p1=`find rdf-processing-toolkit-pkg-parent/rdf-processing-toolkit-pkg-deb-cli/target | grep '\.deb$$'`
	sudo dpkg -i "$$p1"

deb-rere: deb-rebuild deb-reinstall ## rebuild and reinstall deb

