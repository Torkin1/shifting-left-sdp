#!/usr/bin/env bash

dataset="$1"

project_names=("apache/hadoop" "apache/activemq" "apache/camel" "apache/cassandra" "apache/flink" "apache/groovy" "apache/hbase" "apache/hadoop-hdfs" "apache/hive" "apache/ignite" "apache/kafka" "apache/hadoop-mapreduce" "apache/spark" "apache/zeppelin" "apache/zookeeper")
#TODO: check if project keys are correct
project_keys=("HADOOP" "AMQ" "CAMEL" "CASSANDRA" "FLINK" "GROOVY" "HBASE" "HDFS" "HIVE" "IGNITE" "KAFKA" "MAPREDUCE" "SPARK" "ZEPPELIN" "ZOOKEEPER")


set -e

# rename project columns
for i in "${!project_keys[@]}"
do
    sed -i "s|${project_names[$i]}|${project_keys[$i]}|g" "$dataset"
    echo "$0": renamed "${project_names[$i]}" to "${project_keys[$i]}"
done

echo "$0: preprocessing done"

set +e

