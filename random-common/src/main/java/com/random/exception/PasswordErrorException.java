package com.random.exception;

/**
 * 密码错误异常。
 *
 * <p>当用户输入的密码校验不通过时抛出，继承自 {@link BaseException}。</p>
 */
public class PasswordErrorException extends BaseException {

    /**
     * 无参构造方法。
     */
    public PasswordErrorException() {
    }

    /**
     * 带提示信息的构造方法。
     *
     * @param msg 异常提示信息
     */
    public PasswordErrorException(String msg) {
        super(msg);
    }

}
