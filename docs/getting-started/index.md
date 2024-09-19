---
title: Getting Started
has_children: true
nav_order: 20
layout: default
---

# Getting Started



### Downloads

You can download RPT as self-contained JAR bundle, Debian package or RPM package from [RPT's GitHub release page](https://github.com/SmartDataAnalytics/RdfProcessingToolkit/releases).
You can also [build](build) RPT from the source code.

Note, that several aspects of RPT require appropriate [`--add-opens` JVM option declarations](build.html#jvm-options) to function correctly. The bundles have those proconfigured.

### Docker

The quickest way to start an RPT instance is via docker. The container name is `aksw/rpt`. The latest stable version has the tag `latest` whereas the latest development version is available under `latest-dev`:

`docker pull aksw/rpt`

`docker pull aksw/rpt:latest-dev`


For example, a typical invocation of the `integrate` command is as follows:

`docker run -i -p'8642:8642' -v "$(pwd):/data" -w /data aksw/rpt integrate --server YOUR_DATA.ttl`

* `-p'8642:8642'` exposes RPT's port on the host. The order is `-p PortOnHost:PortInsideContainer`.
* `-v "$(pwd):/data"` mounts the current host directory under `/data` in the container
* `-w /data` sets the container's working directory to `/data`
* `YOUR_DATA.ttl` is foremost a path to a file relative to `/data` in the container. Since `/data` is a mount of your host directory, any relative paths inside your host directory should also work inside the container.

Visiting [http://localhost:8642/](http://localhost:8642/) should then show RPT's landing page.

