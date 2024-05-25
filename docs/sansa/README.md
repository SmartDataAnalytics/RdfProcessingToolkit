
# RPT/Sansa

## Compressed Output

Compression using a specific codec can be activated by setting the JVM options `spark.hadoop.mapred.output.compress=true` and `spark.hadoop.mapred.output.compression.codec`.

```bash
JAVA_OPTS="-Dspark.hadoop.mapred.output.compress=true -Dspark.hadoop.mapred.output.compression.codec=org.apache.hadoop.io.compress.BZip2Codec" rpt sansa query mapping.rq --out-file out.nt.bz2 --out-overwrite
```

