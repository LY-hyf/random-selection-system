package com.random.pojo.entity.user;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体类。
 *
 * <p>对应数据库表 sys_user，描述系统中的用户账号信息。</p>
 */
@Data
public class SysUser {

    /** 主键 ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 密码（加密存储） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    private String phone;

    /** 状态：1 启用，0 禁用 */
    private Integer status;

    /** 逻辑删除标记：0 未删除，1 已删除 */
    private Integer deleted;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录更新时间 */
    private LocalDateTime updateTime;
}
