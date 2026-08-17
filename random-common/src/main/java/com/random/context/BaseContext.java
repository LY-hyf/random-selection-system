package com.random.context;

/**
 * 基于 ThreadLocal 的请求上下文工具类。
 *
 * <p>用于在同一请求线程内传递当前登录用户 ID，
 * 避免在方法间显式传递参数，并在请求结束后及时清理防止内存泄漏。</p>
 */
public class BaseContext {

    /** 存储当前登录用户 ID 的线程本地变量 */
    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /**
     * 设置当前线程的用户 ID。
     *
     * @param id 当前登录用户的 ID
     */
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    /**
     * 获取当前线程的用户 ID。
     *
     * @return 当前登录用户的 ID，未设置时返回 null
     */
    public static Long getCurrentId() {
        return threadLocal.get();
    }

    /**
     * 移除当前线程的用户 ID，防止线程复用导致的数据污染或内存泄漏。
     */
    public static void removeCurrentId() {
        threadLocal.remove();
    }

}
