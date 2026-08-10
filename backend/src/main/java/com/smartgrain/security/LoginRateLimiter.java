package com.smartgrain.security;

import com.smartgrain.common.constant.CacheKey;
import com.smartgrain.common.api.ResultCode;
import com.smartgrain.common.exception.BusinessException;
import com.smartgrain.config.SmartGrainProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 登录限流器
 *
 * 策略：
 *   - 同账号失败次数计数（Redis）
 *   - 超过 maxFailCount 则锁定 lockMinutes 分钟
 *   - 成功登录后清零
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRateLimiter {

    private final StringRedisTemplate redis;
    private final SmartGrainProperties properties;

    /** 检查是否被锁定，未锁定返回当前失败次数 */
    public void checkLocked(String username) {
        String lockKey = CacheKey.LOGIN_LOCK + username;
        String locked = redis.opsForValue().get(lockKey);
        if (locked != null) {
            Long ttl = redis.getExpire(lockKey);
            log.warn("账号已被锁定: {} 剩余 {} 秒", username, ttl);
            throw new BusinessException(ResultCode.LOGIN_LOCKED,
                    "账号已锁定，请 " + (ttl == null ? 0 : (ttl + 60) / 60) + " 分钟后重试");
        }
    }

    /** 记录一次失败，达到阈值则锁定 */
    public void recordFail(String username) {
        String countKey = CacheKey.LOGIN_FAIL_COUNT + username;
        Long count = redis.opsForValue().increment(countKey);
        if (count != null && count == 1L) {
            // 第一次失败，设置计数窗口（与锁定时间一致）
            redis.expire(countKey, Duration.ofMinutes(properties.getLogin().getLockMinutes()));
        }
        if (count != null && count >= properties.getLogin().getMaxFailCount()) {
            String lockKey = CacheKey.LOGIN_LOCK + username;
            redis.opsForValue().set(lockKey, "1",
                    Duration.ofMinutes(properties.getLogin().getLockMinutes()));
            redis.delete(countKey);
            log.warn("账号锁定: {} 连续失败 {} 次", username, count);
        }
    }

    /** 登录成功清零 */
    public void recordSuccess(String username) {
        redis.delete(CacheKey.LOGIN_FAIL_COUNT + username);
    }
}
