CWD = $(shell pwd)

POM = -f pom.xml
# Maven Clean Install Skip ; skip tests, javadoc, scaladoc, etc
MS = mvn -DskipTests -Dmaven.javadoc.skip=true -Dskip
MCIS = $(MS) clean install
MCCS = $(MS) clean compile

# Source: https://stackoverflow.com/questions/4219255/how-do-you-get-the-list-of-targets-in-a-makefile
.PHONY: help

.ONESHELL:
help:   ## Show these help instructions
	@sed -rn 's/^([a-zA-Z_-]+):.*?## (.*)$$/"\1" "\2"/p' < $(MAKEFILE_LIST) | xargs printf "make %-20s# %s\n"

distjar: ## Create only the standalone jar-with-dependencies of rpt
	$(MCCS) $(POM) package -Pdist -pl :rdf-processing-toolkit-pkg-uberjar-cli -am $(ARGS)
	file=`find '$(CWD)/rdf-processing-toolkit-pkg-parent/rdf-processing-toolkit-pkg-uberjar-cli/target' -name '*-jar-with-dependencies.jar'`
	printf '\nCreated package:\n\n%s\n\n' "$$file"

rpm-rebuild: ## Rebuild the rpm package (minimal build of only required modules)
	$(MCIS) $(POM) -Prpm -am -pl :rdf-processing-toolkit-pkg-rpm-cli $(ARGS)

rpm-reinstall: ## Reinstall rpm (requires prior build)
	@p1=`find rdf-processing-toolkit-pkg-parent/rdf-processing-toolkit-pkg-rpm-cli/target | grep '\.rpm$$'`
	sudo rpm -i "$$p1"

rpm-rere: rpm-rebuild rpm-reinstall ## Rebuild and reinstall rpm package


deb-rebuild: ## Rebuild the deb package (minimal build of only required modules)
	$(MCIS) $(POM) -Pdeb -am -pl :rdf-processing-toolkit-pkg-deb-cli $(ARGS)

deb-reinstall: ## Reinstall deb (requires prior build)
	@p1=`find rdf-processing-toolkit-pkg-parent/rdf-processing-toolkit-pkg-deb-cli/target | grep '\.deb$$'`
	sudo dpkg -i "$$p1"

deb-rere: deb-rebuild deb-reinstall ## Rebuild and reinstall deb package

