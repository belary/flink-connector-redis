package org.apache.flink.streaming.connectors.redis.table;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.redis.common.config.RedisOptions;
import org.apache.flink.streaming.connectors.redis.common.hanlder.RedisHandlerServices;
import org.apache.flink.streaming.connectors.redis.common.hanlder.RedisMapperHandler;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisMapper;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.apache.flink.streaming.connectors.redis.descriptor.RedisValidator.*;

/**
 * Created by jeff.zou on 2020/9/10.
 */
public class SQLInsertTest {

    public static final String SENTINEL_NODES = "10.26.35.141:6508,10.26.35.73:6508,10.26.35.140:6506";
    public static final String CLUSTERNODES = "10.26.33.71:6667,10.26.33.102:6667,10.26.8:6667";
//    public static final String CLUSTERNODES = "10.11.80.147:7000,10.11.80.147:7001,10.11.80.147:8000,10.11.80.147:8001,10.11.80.147:9000,10.11.80.147:9001";


    @Test
    public void testNoPrimaryKeyInsertSQL() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        EnvironmentSettings environmentSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build();
        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env, environmentSettings);

        String ddl = "create table sink_redis(username VARCHAR, passport VARCHAR) with ( 'connector'='redis', " +
                "'host'='10.26.35.73','port'='6507', 'redis-mode'='single','key-column'='username','value-column'='passport','" +
                REDIS_COMMAND + "'='" + RedisCommand.SET + "')" ;

        tEnv.executeSql(ddl);
        String sql = " insert into sink_redis select * from (values ('mytest', 'test11'))";
        TableResult tableResult = tEnv.executeSql(sql);
        tableResult.getJobClient().get()
                .getJobExecutionResult()
                .get();
        System.out.println(sql);
        System.out.println(ddl);
    }


    /**
     * 这个实现了SET(KEY, VALUE)
     */
    @Test
    public void testSingleInsertHashClusterSQL() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        EnvironmentSettings environmentSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build();
        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env, environmentSettings);

        String ddl = "create table sink_redis(username VARCHAR, level varchar, age varchar) with ( 'connector'='redis', " +
                "'cluster-nodes'='" + CLUSTERNODES + "','redis-mode'='cluster','field-column'='level', 'key-column'='username', 'put-if-absent'='true'," +
                " 'value-column'='age','" +
                REDIS_COMMAND + "'='" + RedisCommand.HSET + "', 'maxIdle'='20', 'minIdle'='10'  )" ;

        tEnv.executeSql(ddl);
        String sql = " insert into sink_redis select * from (values ('test_hash', '3', '15'))";
        TableResult tableResult = tEnv.executeSql(sql);
        tableResult.getJobClient().get()
                .getJobExecutionResult()
                .get();
        System.out.println(sql);



    }


    /**
     * 这个实现了HSET(KEY, FIELD, VALUE)
     */
    @Test
    public void testSentinelClusterSql() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        EnvironmentSettings environmentSettings = EnvironmentSettings.newInstance()
                .useBlinkPlanner().inStreamingMode().build();
        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env ,environmentSettings);

        String ddl =
                "CREATE TABLE test_redis_sentinel_sink1 ( \n" +
                        "    username VARCHAR, \n" +
                        "    level VARCHAR, \n" +
                        "    age VARCHAR" +
                        ") WITH (\n" +
                        "       'connector'         = 'redis', \n" +
                        "       'redis-mode'        = 'sentinel', \n" +
                        "       'sentinels.info'    = '10.26.35.141:6508,10.26.35.73:6508,10.26.35.140:6506',\n" +
                        "       'master.name'       = 'sentinel-10.26.35.73-6507', \n" +
                        "       'key-column'        = 'username',\n" +
                        "       'field-column'      = 'level',\n" +
                        "       'value-column'      = 'age',\n" +
                        "       'command'           = 'HSET'\n" +
                        "    )";
        tEnv.executeSql(ddl);

        String sql = " insert into test_redis_sentinel_sink1 select * from (values ('test_fc:level:age', '3', '19'))";
        TableResult tableResult = tEnv.executeSql(sql);
        tableResult.getJobClient().get()
                .getJobExecutionResult()
                .get();
        System.out.println(sql);

    }
}