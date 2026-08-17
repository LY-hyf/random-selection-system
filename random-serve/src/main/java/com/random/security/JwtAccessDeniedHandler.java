package com.random.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.random.result.Result;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT 无权限访问处理器。
 *
 * <p>当已认证用户访问无权限的资源时，返回 403 及标准 JSON 错误信息。</p>
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * 处理无权限访问。
     *
     * @param request               请求对象
     * @param response              响应对象
     * @param accessDeniedException 访问被拒绝异常
     * @throws IOException 写出响应失败时抛出
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Result result = Result.error("权限不足");
        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

}
