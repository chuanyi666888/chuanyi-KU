package com.smartgrain.domain.backup.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 备份统计视图对象
 */
@Data
@Schema(description = "备份统计")
public class BackupStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "MySQL 备份数量")
    private int mysqlCount;

    @Schema(description = "MySQL 备份总大小(字节)")
    private long mysqlTotalSize;

    @Schema(description = "KingbaseES 备份数量")
    private int kingbaseCount;

    @Schema(description = "KingbaseES 备份总大小(字节)")
    private long kingbaseTotalSize;

    @Schema(description = "最近一次 MySQL 备份时间")
    private String lastMysqlBackupTime;

    @Schema(description = "最近一次 KingbaseES 备份时间")
    private String lastKingbaseBackupTime;

    @Schema(description = "今日备份记录")
    private List<BackupRecordVO> todayBackups;

    @Schema(description = "备份根目录")
    private String mysqlBackupRoot;

    @Schema(description = "KingbaseES 备份根目录")
    private String kingbaseBackupRoot;
}
