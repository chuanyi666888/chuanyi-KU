package com.smartgrain.service.backup;

import com.smartgrain.domain.backup.vo.BackupRecordVO;
import com.smartgrain.domain.backup.vo.BackupStatsVO;

import java.util.List;
import java.util.Map;

/**
 * 备份管理服务
 */
public interface BackupService {

    /**
     * 列出指定数据库的备份记录
     *
     * @param dbKind    mysql / kingbase
     * @param category  可选: daily / weekly / monthly / binlog / wal / base
     * @param limit     每个分类最多返回条数
     */
    List<BackupRecordVO> listBackups(String dbKind, String category, int limit);

    /**
     * 获取今日备份列表
     */
    List<BackupRecordVO> listTodayBackups();

    /**
     * 备份统计概览
     */
    BackupStatsVO stats();

    /**
     * 校验指定备份文件（MD5）
     */
    Map<String, Object> verify(String dbKind, String category, String backupDate);

    /**
     * 触发备份（异步执行）
     *
     * @param dbKind mysql / kingbase
     * @param action full / incremental / cleanup / verify / all
     * @return 任务执行信息
     */
    Map<String, Object> trigger(String dbKind, String action);

    /**
     * 查询任务执行状态
     *
     * @param taskId trigger 返回的 taskId
     */
    Object getTask(String taskId);
}
