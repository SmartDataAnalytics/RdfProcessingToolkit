---
title: SPARQL Extensions
nav_order: 40
has_children: true
layout: default
---

# AI SPARQL Extensions

This section summarizes how to combine LLMs with SPARQL.
There exist several docker containers for running HTTP servers with APIs backed by LLMs.
Currently RPT supports HTTP bindings for [Ollama](https://github.com/ollama/ollama).

## Ollama Integration

The ollama bindings are implemented as [macros](../sparql-extensions/macros.md) that wrap RPT's built-in HTTP client SPARQL function.
See the section [Ollama Quick Start](#ollama-quick-start) for getting a local ollama HTTP service instance running using docker.

### SPARQL Function Reference

```
PREFIX ollama: <https://w3id.org/aksw/norse#ollama.>

SELECT * {
  BIND(ollama.complete("http://localhost:11434/v1/chat/completions", "llama3.1", "What is the capital of Germany?") AS ?completedText)
  BIND(ollama.completeJson("http://localhost:11434/v1/chat/completions", "llama3.1", "What is the capital of Germany? Answer a JSON object whose key 'capital' has the appropriate value.") AS ?completedJson)
  BIND(ollama.embed("http://localhost:11434/api/embeddings", "llama3.1", "The capital of Germany is Berlin.") AS ?jsonArray)  
}
```

### <a id="ollama-quick-start"></a>Ollama Quick Start

1. (Needed for GPUs) Make sure the docker can access the GPUs. For Nvidia GPUs, the following package must be installed:

```bash
sudo apt install nvidia-container-toolkit
```

2. Start an ollama docker container with the name `ollama`. Mount the model folder from the host `$HOME/ollama` into the container's preset folder `/root/.ollama`. The option `--gpus all` tries to leverage the host's GPUs.

```bash
docker run --rm -d -v "$HOME/ollama:/root/.ollama" -p 11434:11434 --gpus all --name ollama ollama/ollama
```

3. Set up a model from the ollama registry:

```bash
docker exec -it ollama ollama run llama3.1
```


### Macro Definitions

* The method `ollama.complete` macro is 

```sparql
PREFIX norse: <https://w3id.org/aksw/norse#>

SELECT * {
  BIND(norse:json.object(
      "model", "llama3.1",
      "prompt", "What is the capital of Germany?"
  ) AS ?bodyJson)  
  BIND(STR(?bodyJson) AS ?bodyStr)
  BIND(norse:url.fetchSpec("http://localhost:11434/api/embeddings",
      "m", "POST", "h.ContentType", "application/json", "b", ?bodyStr,
    "cto", 60000, "rto", 60000) AS ?fetchSpec)
  BIND(norse:url.fetch(?fetchSpec) AS ?responseJson)
  BIND(norse:json.path(?responseJson, "$.choices[0].message.content") AS ?response)
}
```

* The method `ollama.embed` is effectively a macro for the following fragment, where url, model and prompt and parameters:

```sparql
PREFIX norse: <https://w3id.org/aksw/norse#>

SELECT * {
  BIND(norse:json.object(
      "model", "llama3.1",
      "prompt", "What is the capital of Germany?"
  ) AS ?bodyJson)
  BIND(STR(?bodyJson) AS ?bodyStr)
  BIND(norse:url.fetchSpec("http://localhost:11434/api/embeddings",
      "m", "POST", "h.ContentType", "application/json", "b", ?bodyStr,
    "cto", 60000, "rto", 60000) AS ?fetchSpec)
  BIND(norse:url.fetch(?fetchSpec) AS ?responseJson)
  BIND(norse:json.path(?responseJson, "$.embedding") AS ?resultArray)
}
```


