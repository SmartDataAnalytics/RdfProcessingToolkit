CWD = $(shell pwd)

POM = -f pom.xml
# Maven Clean Install Skip ; skip tests, javadoc, scaladoc, etc
MS = mvn -DskipTests -Dmaven.javadoc.skip=true -Dskip
MCIS = $(MS) clean install
MCCS = $(MS) clean compile

VER = $(error specify VER=releasefile-name e.g. VER=1.9.7-rc2)
loud = echo "@@" $(1);$(1)

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
	sudo rpm -U "$$p1"

rpm-rere: rpm-rebuild rpm-reinstall ## Rebuild and reinstall rpm package


deb-rebuild: ## Rebuild the deb package (minimal build of only required modules)
	$(MCIS) $(POM) -Pdeb -am -pl :rdf-processing-toolkit-pkg-deb-cli $(ARGS)

deb-reinstall: ## Reinstall deb (requires prior build)
	@p1=`find rdf-processing-toolkit-pkg-parent/rdf-processing-toolkit-pkg-deb-cli/target | grep '\.deb$$'`
	sudo dpkg -i "$$p1"

deb-rere: deb-rebuild deb-reinstall ## Rebuild and reinstall deb package


docker: ## Build Docker image
	$(MCIS) $(POM) -am -pl :rdf-processing-toolkit-pkg-docker-cli $(ARGS)
	cd rdf-processing-toolkit-pkg-parent/rdf-processing-toolkit-pkg-docker-cli && $(MS) $(ARGS) jib:dockerBuild && cd ../..

release-bundle: ## Create files for Github upload
	@set -eu
	ver=$(VER)
	$(call loud,$(MAKE) deb-rebuild)
	p1=`find rdf-processing-toolkit-pkg-parent/rdf-processing-toolkit-pkg-deb-cli/target | grep '\.deb$$'`
	$(call loud,cp "$$p1" "rpt-$${ver/-/\~}.deb")
	$(call loud,$(MAKE) rpm-rebuild)
	p1=`find rdf-processing-toolkit-pkg-parent/rdf-processing-toolkit-pkg-rpm-cli/target | grep '\.rpm$$'`
	$(call loud,cp "$$p1" "rpt-$$ver.rpm")
	$(call loud,$(MAKE) distjar)
	file=`find '$(CWD)/rdf-processing-toolkit-pkg-parent/rdf-processing-toolkit-pkg-uberjar-cli/target' -name '*-jar-with-dependencies.jar'`
	$(call loud,cp "$$file" "rpt-$$ver.jar")
	$(call loud,$(MAKE) docker)
	$(call loud,docker tag aksw/rpt aksw/rpt:$$ver)
	$(call loud,gh release create v$$ver "rpt-$${ver/-/\~}.deb" "rpt-$$ver.rpm" "rpt-$$ver.jar")
	$(call loud,docker push aksw/rpt:$$ver)
	$(call loud,docker push aksw/rpt)
