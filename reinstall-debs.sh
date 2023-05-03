#!/bin/sh
cd "$(dirname "$0")"

p1=`find rdf-processing-toolkit-pkg-parent/rdf-processing-toolkit-pkg-deb-cli/target | grep '\.deb$'`

sudo dpkg -i "$p1"

