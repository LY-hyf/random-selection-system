package com.random.aspect;

import com.random.annotation.Log;
import com.random.context.BaseContext;
import com.random.mapper.log.SysOperationLogMapper;
import com.random.pojo.entity.SysOperationLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 操作日志切面，拦截标注了 {@link Log} 注解的方法并记录操作日志。
 */
@Aspect
@Component
@Slf4j
public class OperationLogAspect {

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    @Around("@annotation(logAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, Log logAnnotation) throws Throwable {
        try {
            Object result = joinPoint.proceed();
            saveLog(joinPoint, logAnnotation, null);
            return result;
        } catch (Throwable e) {
            saveLog(joinPoint, logAnnotation, e.getMessage());
            throw e;
        }
    }

    /**
     * 保存操作日志，记录操作模块、操作内容、请求地址、IP 及执行结果。
     */
    private void saveLog(ProceedingJoinPoint joinPoint, Log logAnnotation, String errorMessage) {
        try {
            HttpServletRequest request = getRequest();
            SysOperationLog operationLog = new SysOperationLog();
            operationLog.setUserId(BaseContext.getCurrentId());
            operationLog.setUsername(getUsername());
            operationLog.setModule(logAnnotation.module());
            operationLog.setOperation(logAnnotation.operation());
            operationLog.setRequestUrl(request != null ? request.getRequestURI() : null);
            operationLog.setRequestMethod(request != null ? request.getMethod() : null);
            operationLog.setIp(getIp(request));
            operationLog.setResult(errorMessage == null ? 1 : 0);
            operationLog.setErrorMessage(errorMessage);
            operationLog.setCreateTime(LocalDateTime.now());
            sysOperationLogMapper.insert(operationLog);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }

    /**
     * 获取当前 HTTP 请求对象，非 Web 上下文时返回 null。
     */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 获取当前登录用户名。
     */
    private String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }

    /**
     * 获取客户端真实 IP，兼容 X-Forwarded-For、X-Real-IP 等代理转发头。
     */
    private String getIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

}
