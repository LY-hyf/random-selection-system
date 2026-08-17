package com.random.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 后端统一返回结果
 */
@Data
public class Result<T> implements Serializable {

    /** 状态码：1 表示成功，0 表示失败 */
    private Integer code;

    /** 提示信息 */
    private String msg;

    /** 返回的数据 */
    private T data;

    /**
     * 构建一个不带数据的成功结果。
     *
     * @param <T> 数据类型
     * @return 状态码为 1 的成功结果
     */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.code = 1;
        return result;
    }

    /**
     * 构建一个带数据的成功结果。
     *
     * @param object 要返回的数据
     * @param <T>    数据类型
     * @return 状态码为 1 且携带数据的成功结果
     */
    public static <T> Result<T> success(T object) {
        Result<T> result = new Result<>();
        result.data = object;
        result.code = 1;
        return result;
    }


    /**
     * 构建一个失败结果。
     *
     * @param msg 失败提示信息
     * @param <T> 数据类型
     * @return 状态码为 0 的失败结果
     */
    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.msg = msg;
        result.code = 0;
        return result;
    }

}
