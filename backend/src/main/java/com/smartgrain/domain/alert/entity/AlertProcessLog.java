package com.smartgrain.domain.alert.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("alert_process_log")
public class AlertProcessLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long alertId;
    private String action;
    private String fromStatus;
    private String toStatus;
    private String comment;
    private LocalDateTime processAt;
    private Long processBy;
}
