package com.smartgrain.domain.alert.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
public class AlertStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 总告警数 */
    private Long total;
    /** 待处理 */
    private Long pending;
    /** 处理中 */
    private Long processing;
    /** 已解决 */
    private Long resolved;
    /** 已忽略 */
    private Long ignored;

    /** 按严重等级 1/2/3 */
    private Map<Integer, Long> bySeverity;
    /** 按传感器类型 */
    private Map<String, Long> bySensorType;
    /** 按粮库 */
    private Map<Long, Long> byDepot;
    /** 近 7 日每日告警数 */
    private Map<String, Long> dailyTrend;
}
