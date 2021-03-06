package com.whl.ReaderApp.tools;

/**
 * Redis生成key用
 *
 * @author whl
 */
public class RedisKey {
    public static final String USER = "Users";
    public static final String BOOK = "Books";
    public static final String BOOK_CHILD = "%s:%s";
    public static final String BOOK_SEARCH_HISTORY = "BookSearchHistory:%s";
    public static final String BOOK_SHOP = "BookShop:%s";
    public static final String BOOK_SHOP_CHILD = "%s:%s";

    public static String of(String key, Object... args) {
        return String.format(key, args);
    }
}
