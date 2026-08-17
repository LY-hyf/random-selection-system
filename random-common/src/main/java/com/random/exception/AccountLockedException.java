package com.random.exception;

/**
 * 账号被锁定异常。
 *
 * <p>当用户账号处于禁用/锁定状态时抛出，继承自 {@link BaseException}。</p>
 */
public class AccountLockedException extends BaseException {

    /**
     * 无参构造方法。
     */
    public AccountLockedException() {
    }

    /**
     * 带提示信息的构造方法。
     *
     * @param msg 异常提示信息
     */
    public AccountLockedException(String msg) {
        super(msg);
    }

}
