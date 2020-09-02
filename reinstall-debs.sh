#!/bin/sh
cd "$(dirname "$0")"

p1=`find rdf-processing-toolkit-debian-cli/target | grep '\.deb$'`

sudo dpkg -i "$p1"

