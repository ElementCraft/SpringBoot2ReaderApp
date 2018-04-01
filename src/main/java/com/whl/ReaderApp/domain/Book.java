package com.whl.ReaderApp.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 书本类
 *
 * @author whl
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Book {

    /**
     * 书名
     */
    private String name;

    /**
     * 作者
     */
    private String author;

    /**
     * 说明
     */
    private String brief;

    /**
     * 封面路径
     */
    private String imgIcon;
}
