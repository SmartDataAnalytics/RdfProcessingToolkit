---
title: Building from Source
parent: Getting Started
has_children: false
nav_order: 10
layout: default
---

# Building RPT from Source

The build requires maven.

For convenience, a [Makefile](https://github.com/SmartDataAnalytics/RdfProcessingToolkit/blob/develop/Makefile) which defines essential goals for common tasks.
To build a "jar-with-dependencies" use the `distjar` goal. The path to the created jar bundle is shown when the build finishes.
In order to build and and install a deb or rpm package use the `deb-rere` or `rpm-rere` goals, respectively.

```
$ make

make help                # Show these help instructions
make distjar             # Create only the standalone jar-with-dependencies of rpt
make rpm-rebuild         # Rebuild the rpm package (minimal build of only required modules)
make rpm-reinstall       # Reinstall rpm (requires prior build)
make rpm-rere            # Rebuild and reinstall rpm package
make deb-rebuild         # Rebuild the deb package (minimal build of only required modules)
make deb-reinstall       # Reinstall deb (requires prior build)
make deb-rere            # Rebuild and reinstall deb package
make docker              # Build Docker image
make release-bundle      # Create files for Github upload
```



A docker image is available at https://registry.hub.docker.com/r/aksw/rpt

The docker image can be built with a custom tag by setting the property `docker.tag`.
The default for `docker.tag` is `${docker.tag.prefix}${project.version}`, where `docker.tag.prefix` defaults to the empty string.
When only setting `docker.tag.prefix` to e.g. `myfork-` then the tag will have the form `myfork-1.2.3-SNAPSHOT`.

```bash
make docker

# Set a custom prefix to which the version will be appended:
make docker ARGS='-D"docker.tag.prefix=experimental-"'

# Set the tag to a specific value
make docker ARGS='-D"docker.tag=latest-dev"'
```



## JVM Options

Since Java 17, RPT requires a set of `--add-opens` declarations in order for all of its various aspects to function correctly. These declarations are shown below. RPT's package builds (such as `uberjar`, `.deb`, `.rpm`, `docker`) already include these declarations. However, when creating custom bundles of the code, you need to be aware that these declarations are needed for correct functioning of all of RPT's aspects.

```bash
#!/bin/sh

MAIN_CLASS="org.aksw.rdf_processing_toolkit.cli.main.MainCliRdfProcessingToolkit"

# Extra options for Java 17; Source: 
EXTRA_OPTS="--add-opens=java.base/java.lang=ALL-UNNAMED \
    --add-opens=java.base/java.lang.invoke=ALL-UNNAMED \
    --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
    --add-opens=java.base/java.io=ALL-UNNAMED \
    --add-opens=java.base/java.net=ALL-UNNAMED \
    --add-opens=java.base/java.nio=ALL-UNNAMED \
    --add-opens=java.base/java.util=ALL-UNNAMED \
    --add-opens=java.base/java.util.concurrent=ALL-UNNAMED \
    --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED \
    --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
    --add-opens=java.base/sun.nio.cs=ALL-UNNAMED \
    --add-opens=java.base/sun.security.action=ALL-UNNAMED \
    --add-opens=java.base/sun.util.calendar=ALL-UNNAMED \
    --add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED"

SCRIPTING_OPTS="-Djena:scripting=true -Dnashorn.args=--language=es6"

java $SCRIPTING_OPTS $JAVA_OPTS -jar rpt-jar-with-dependencies.jar "$MAIN_CLASS" "$@"

```

