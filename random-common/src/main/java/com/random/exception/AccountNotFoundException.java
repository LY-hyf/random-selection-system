package com.random.exception;

/**
 * 账号不存在异常。
 *
 * <p>当查询不到指定账号时抛出，继承自 {@link BaseException}。</p>
 */
public class AccountNotFoundException extends BaseException {

    /**
     * 无参构造方法。
     */
    public AccountNotFoundException() {
    }

    /**
     * 带提示信息的构造方法。
     *
     * @param msg 异常提示信息
     */
    public AccountNotFoundException(String msg) {
        super(msg);
    }

}
