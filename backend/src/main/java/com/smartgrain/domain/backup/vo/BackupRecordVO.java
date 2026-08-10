package com.smartgrain.domain.backup.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 备份记录视图对象
 */
@Data
@Schema(description = "备份记录")
public class BackupRecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "备份日期 yyyyMMdd")
    private String backupDate;

    @Schema(description = "备份类型: full / incremental / basebackup")
    private String backupType;

    @Schema(description = "分类: daily / weekly / monthly / binlog / wal / base")
    private String category;

    @Schema(description = "数据库类型: mysql / kingbase")
    private String dbKind;

    @Schema(description = "备份文件名")
    private String backupFile;

    @Schema(description = "备份文件大小(字节)")
    private long backupSize;

    @Schema(description = "备份文件大小(可读)")
    private String backupSizeReadable;

    @Schema(description = "MD5 校验和")
    private String backupMd5;

    @Schema(description = "备份时间")
    private String backupTime;

    @Schema(description = "数据库名")
    private String database;

    @Schema(description = "主机")
    private String host;

    @Schema(description = "本地路径")
    private String localPath;

    @Schema(description = "是否通过校验")
    private Boolean verified;
}
