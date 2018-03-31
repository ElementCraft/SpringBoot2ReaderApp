package com.whl.ReaderApp.tools;

/**
 * Redis生成key用
 *
 * @author whl
 */
public class RedisKey {
    public static final String USER = "Users";

    public static String of(String key, Object... args){
        return String.format(key, args);
    }
}
