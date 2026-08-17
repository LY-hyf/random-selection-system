package com.random.exception;

/**
 * 登录失败异常。
 *
 * <p>当登录或相关认证流程失败时抛出，继承自 {@link BaseException}。</p>
 */
public class LoginFailedException extends BaseException {

    /**
     * 带提示信息的构造方法。
     *
     * @param msg 异常提示信息
     */
    public LoginFailedException(String msg) {
        super(msg);
    }

}
