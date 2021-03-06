package com.xq.apitest.sinktest;

import com.xq.apitest.pojo.SensorReading;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.parquet.avro.ParquetAvroWriters;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.BasePathBucketAssigner;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.DateTimeBucketAssigner;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.OnCheckpointRollingPolicy;

import java.net.URL;

public class StreamingFileSinkTest {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        URL resource = StreamingFileSinkTest.class.getResource("/sensor.txt");
        DataStreamSource<String> inputStream = env.readTextFile(resource.getPath().toString());
        SingleOutputStreamOperator<SensorReading> dataStream = inputStream.map(new MapFunction<String, SensorReading>() {
            @Override
            public SensorReading map(String value) throws Exception {
                String[] split = value.split(",");
                return new SensorReading(split[0].trim(), Long.parseLong(split[1].trim()), Double.parseDouble(split[2].trim()));
            }
        });

        dataStream.print();

        StreamingFileSink<SensorReading> sink1 = StreamingFileSink.forRowFormat(
                new Path("D:\\code\\FlinkTutorial_1.10_New\\src\\main\\resources\\out_1"),
                new SimpleStringEncoder<SensorReading>("UTF-8")
        ).withBucketAssigner(new BasePathBucketAssigner<>())
         .withRollingPolicy(DefaultRollingPolicy.builder().build())
         .build();

        StreamingFileSink<SensorReading> sink2 = StreamingFileSink.forRowFormat(
                new Path("D:\\code\\FlinkTutorial_1.10_New\\src\\main\\resources\\out_2"),
                new SimpleStringEncoder<SensorReading>("UTF-8")
        ).withBucketAssigner(new DateTimeBucketAssigner<>())
         .withRollingPolicy(DefaultRollingPolicy.builder().build())
         .build();

        StreamingFileSink<SensorReading> sink3 = StreamingFileSink.forRowFormat(
                new Path("D:\\code\\FlinkTutorial_1.10_New\\src\\main\\resources\\out_3")
                ,new SimpleStringEncoder<SensorReading>()
        ).build();

        StreamingFileSink<SensorReading> sink4 = StreamingFileSink.forBulkFormat(
                new Path("D:\\code\\FlinkTutorial_1.10_New\\src\\main\\resources\\out_4")
                , ParquetAvroWriters.forReflectRecord(SensorReading.class)
        ).withBucketAssigner(new DateTimeBucketAssigner<>())
                .withRollingPolicy(OnCheckpointRollingPolicy.build())
                .build();


        /**
         * 报错：The writeAsCsv() method can only be used on data streams of tuples.
         */
//        map.writeAsCsv("D:\\code\\FlinkTutorial_1.10_New\\src\\main\\resources\\out_4.txt");

        dataStream.addSink(sink4);

        env.execute("file sink test");


    }
}
