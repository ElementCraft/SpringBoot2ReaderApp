package com.whl.ReaderApp.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户类
 *
 * @author whl
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    String account;

    String password;
}
