package com.random.exception;

/**
 * 业务异常基类
 */
public class BaseException extends RuntimeException {

    /**
     * 无参构造方法。
     */
    public BaseException() {
    }

    /**
     * 带提示信息的构造方法。
     *
     * @param msg 异常提示信息
     */
    public BaseException(String msg) {
        super(msg);
    }

}
