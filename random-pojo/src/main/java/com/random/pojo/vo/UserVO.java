package com.random.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户视图对象。
 */
@Data
public class UserVO {

    /** 用户 ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    private String phone;

    /** 状态：1 启用，0 禁用 */
    private Integer status;

    /** 角色名称集合 */
    private List<String> roleNames;

    /** 创建时间 */
    private LocalDateTime createTime;
}
