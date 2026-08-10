package com.smartgrain.aspect;

import com.smartgrain.common.util.IpUtils;
import com.smartgrain.common.util.SecurityUtils;
import com.smartgrain.domain.audit.entity.AuditLog;
import com.smartgrain.service.audit.AuditLogService;
import com.smartgrain.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * 审计日志 AOP
 *
 * 自动拦截 Controller 层的 POST/PUT/DELETE 写操作，
 * 记录操作人、IP、URL、参数、结果、耗时
 *
 * 注：参数会经过 toString() 后截断，敏感字段(password 等)由各 DTO 的 @ToString 控制
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    @Around("execution(* com.smartgrain.controller..*.*(..)) && " +
            "(@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            " @annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            " @annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        AuditLog logEntity = new AuditLog();
        logEntity.setModule(extractModule(pjp));
        logEntity.setOperation(extractOperation(pjp));

        // 请求信息
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            logEntity.setRequestUrl(req.getRequestURI());
            logEntity.setMethod(req.getMethod());
            logEntity.setRequestIp(IpUtils.getClientIp(req));
        }

        // 操作人
        try {
            if (SecurityUtils.isAuthenticated()) {
                UserPrincipal user = SecurityUtils.getCurrentUser();
                logEntity.setOperatorId(user.getId());
                logEntity.setOperator(user.getUsername());
            }
        } catch (Exception ignored) {
        }

        // 参数（脱敏）
        try {
            String args = Arrays.toString(pjp.getArgs());
            if (args != null && args.length() > 1000) {
                args = args.substring(0, 1000) + "...";
            }
            logEntity.setRequestBody(args);
            logEntity.setTarget(extractTarget(pjp));
        } catch (Exception ignored) {
        }

        Object result;
        try {
            result = pjp.proceed();
            logEntity.setResult(1);
        } catch (Throwable e) {
            logEntity.setResult(0);
            String msg = e.getMessage();
            if (msg != null && msg.length() > 500) {
                msg = msg.substring(0, 500);
            }
            logEntity.setErrorMsg(msg);
            throw e;
        } finally {
            logEntity.setCostMs((int) (System.currentTimeMillis() - start));
            // 异步写入，不影响主流程
            auditLogService.record(logEntity);
        }
        return result;
    }

    private String extractModule(ProceedingJoinPoint pjp) {
        String className = pjp.getTarget().getClass().getSimpleName();
        if (className.endsWith("Controller")) {
            return className.substring(0, className.length() - "Controller".length());
        }
        return className;
    }

    private String extractOperation(ProceedingJoinPoint pjp) {
        String method = pjp.getSignature().getName();
        if (method.startsWith("create") || method.startsWith("add") || method.startsWith("save")) return "CREATE";
        if (method.startsWith("update") || method.startsWith("edit") || method.startsWith("modify")) return "UPDATE";
        if (method.startsWith("delete") || method.startsWith("remove")) return "DELETE";
        if (method.startsWith("login")) return "LOGIN";
        if (method.startsWith("logout")) return "LOGOUT";
        if (method.startsWith("assign")) return "ASSIGN";
        if (method.startsWith("reset")) return "RESET";
        return method.toUpperCase();
    }

    private String extractTarget(ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();
        if (args == null || args.length == 0) return null;
        Object first = args[0];
        return first == null ? null : first.getClass().getSimpleName();
    }
}
