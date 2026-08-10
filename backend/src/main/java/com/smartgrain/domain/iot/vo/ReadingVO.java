package com.smartgrain.domain.iot.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReadingVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long sensorId;
    private String sensorCode;
    private String sensorName;
    private String sensorType;
    private String unit;
    private BigDecimal value;
    private LocalDateTime readingAt;
}
