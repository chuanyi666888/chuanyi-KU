package com.smartgrain.service.backup.impl;

import com.smartgrain.common.api.ResultCode;
import com.smartgrain.common.exception.BusinessException;
import com.smartgrain.config.SmartGrainProperties;
import com.smartgrain.domain.backup.vo.BackupRecordVO;
import com.smartgrain.domain.backup.vo.BackupStatsVO;
import com.smartgrain.service.backup.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * 备份管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {

    private final SmartGrainProperties properties;

    /** 任务执行记录（进程级内存，重启后丢失） */
    private static final Map<String, TaskInfo> TASK_STORE = new ConcurrentHashMap<>();

    /** MySQL 各分类目录 */
    private static final String[] MYSQL_CATEGORIES = {"daily", "weekly", "monthly", "binlog"};
    /** KingbaseES 各分类目录 */
    private static final String[] KB_CATEGORIES = {"daily", "weekly", "monthly", "wal", "base"};

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public List<BackupRecordVO> listBackups(String dbKind, String category, int limit) {
        String root = rootOf(dbKind);
        String[] categories = categoriesOf(dbKind);
        if (category != null && !category.isBlank()) {
            categories = new String[]{category};
        }
        final int effectiveLimit = limit <= 0 ? 50 : Math.min(limit, 500);

        List<BackupRecordVO> result = new ArrayList<>();
        for (String cat : categories) {
            File catDir = new File(root, cat);
            if (!catDir.isDirectory()) continue;
            // 列出日期子目录，按名称倒序（最新的在前）
            File[] dateDirs = catDir.listFiles(File::isDirectory);
            if (dateDirs == null) continue;
            List<File> sorted = new ArrayList<>(List.of(dateDirs));
            sorted.sort((a, b) -> b.getName().compareTo(a.getName()));
            for (File d : sorted) {
                if (result.size() >= effectiveLimit) break;
                BackupRecordVO vo = parseMeta(d, dbKind, cat);
                if (vo != null) result.add(vo);
            }
            if (result.size() >= effectiveLimit) break;
        }
        return result;
    }

    @Override
    public List<BackupRecordVO> listTodayBackups() {
        String today = LocalDate.now().format(YYYYMMDD);
        List<BackupRecordVO> all = new ArrayList<>();
        for (String cat : MYSQL_CATEGORIES) {
            BackupRecordVO vo = parseMeta(new File(properties.getBackup().getMysqlRoot(), cat + "/" + today), "mysql", cat);
            if (vo != null) all.add(vo);
        }
        for (String cat : KB_CATEGORIES) {
            BackupRecordVO vo = parseMeta(new File(properties.getBackup().getKingbaseRoot(), cat + "/" + today), "kingbase", cat);
            if (vo != null) all.add(vo);
        }
        return all;
    }

    @Override
    public BackupStatsVO stats() {
        BackupStatsVO stats = new BackupStatsVO();
        stats.setMysqlBackupRoot(properties.getBackup().getMysqlRoot());
        stats.setKingbaseBackupRoot(properties.getBackup().getKingbaseRoot());

        // MySQL 统计
        List<BackupRecordVO> mysqlBackups = listBackups("mysql", null, 1000);
        stats.setMysqlCount(mysqlBackups.size());
        stats.setMysqlTotalSize(mysqlBackups.stream().mapToLong(BackupRecordVO::getBackupSize).sum());
        stats.setLastMysqlBackupTime(mysqlBackups.stream()
                .map(BackupRecordVO::getBackupTime)
                .max(String::compareTo)
                .orElse(null));

        // KingbaseES 统计
        List<BackupRecordVO> kbBackups = listBackups("kingbase", null, 1000);
        stats.setKingbaseCount(kbBackups.size());
        stats.setKingbaseTotalSize(kbBackups.stream().mapToLong(BackupRecordVO::getBackupSize).sum());
        stats.setLastKingbaseBackupTime(kbBackups.stream()
                .map(BackupRecordVO::getBackupTime)
                .max(String::compareTo)
                .orElse(null));

        stats.setTodayBackups(listTodayBackups());
        return stats;
    }

    @Override
    public Map<String, Object> verify(String dbKind, String category, String backupDate) {
        Map<String, Object> result = new LinkedHashMap<>();
        String root = rootOf(dbKind);
        File dir = new File(root, category + "/" + backupDate);
        if (!dir.isDirectory()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "备份目录不存在: " + dir.getAbsolutePath());
        }
        BackupRecordVO vo = parseMeta(dir, dbKind, category);
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "未找到 META.txt 或解析失败");
        }
        File backupFile = new File(dir, vo.getBackupFile());
        boolean exists = backupFile.isFile();
        boolean md5Ok = false;
        if (exists) {
            try {
                String actual = md5(backupFile.toPath());
                md5Ok = actual.equalsIgnoreCase(vo.getBackupMd5());
                result.put("actualMd5", actual);
            } catch (IOException e) {
                result.put("verifyError", e.getMessage());
            }
        }
        result.put("record", vo);
        result.put("fileExists", exists);
        result.put("md5Matched", md5Ok);
        result.put("verified", exists && md5Ok);
        return result;
    }

    @Override
    public Map<String, Object> trigger(String dbKind, String action) {
        if (!properties.getBackup().isEnableTrigger()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "已禁用 API 触发备份脚本");
        }
        String scriptName = switch (dbKind.toLowerCase()) {
            case "mysql" -> "mysql-backup.sh";
            case "kingbase", "kingbasees" -> "kingbase-backup.sh";
            default -> throw new BusinessException(ResultCode.PARAM_INVALID, "未知 dbKind: " + dbKind);
        };
        String effectiveAction = (action == null || action.isBlank()) ? "full" : action;
        Path script = Paths.get(properties.getBackup().getScriptDir(), scriptName);
        if (!Files.isExecutable(script)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "脚本不存在或不可执行: " + script);
        }

        String taskId = "bk_" + System.currentTimeMillis();
        TaskInfo info = new TaskInfo(taskId, dbKind, effectiveAction, "RUNNING", System.currentTimeMillis());
        TASK_STORE.put(taskId, info);

        // 异步执行
        runScriptAsync(taskId, script, effectiveAction);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", taskId);
        result.put("status", "RUNNING");
        result.put("message", "任务已提交，可通过 GET /api/v1/backup/tasks/" + taskId + " 查询进度");
        return result;
    }

    @Override
    public Object getTask(String taskId) {
        TaskInfo info = TASK_STORE.get(taskId);
        if (info == null) throw new BusinessException(ResultCode.NOT_FOUND, "任务不存在: " + taskId);
        return info;
    }

    @Async
    public void runScriptAsync(String taskId, Path script, String action) {
        TaskInfo info = TASK_STORE.get(taskId);
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", script.toString(), action);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line).append('\n');
                    if (out.length() > 8192) {
                        out.setLength(8192);
                        out.append("...(truncated)");
                        break;
                    }
                }
            }
            boolean finished = p.waitFor(properties.getBackup().getScriptTimeoutSec(), TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                info.setStatus("TIMEOUT");
                info.setOutput(out.toString());
                return;
            }
            int code = p.exitValue();
            info.setExitCode(code);
            info.setOutput(out.toString());
            info.setStatus(code == 0 ? "SUCCESS" : "FAILED");
            info.setFinishTime(System.currentTimeMillis());
        } catch (Exception e) {
            log.error("备份脚本执行异常 taskId={}", taskId, e);
            info.setStatus("ERROR");
            info.setOutput(e.getMessage());
            info.setFinishTime(System.currentTimeMillis());
        }
    }

    // ===== 私有辅助 =====

    private String rootOf(String dbKind) {
        return "kingbase".equalsIgnoreCase(dbKind)
                ? properties.getBackup().getKingbaseRoot()
                : properties.getBackup().getMysqlRoot();
    }

    private String[] categoriesOf(String dbKind) {
        return "kingbase".equalsIgnoreCase(dbKind) ? KB_CATEGORIES : MYSQL_CATEGORIES;
    }

    /**
     * 解析 META.txt 文件生成 VO
     */
    private BackupRecordVO parseMeta(File dateDir, String dbKind, String category) {
        File meta = new File(dateDir, "META.txt");
        if (!meta.isFile()) return null;

        BackupRecordVO vo = new BackupRecordVO();
        vo.setDbKind(dbKind);
        vo.setCategory(category);
        vo.setBackupDate(dateDir.getName());
        vo.setLocalPath(dateDir.getAbsolutePath());

        try (Stream<String> lines = Files.lines(meta.toPath(), StandardCharsets.UTF_8)) {
            lines.forEach(line -> {
                int idx = line.indexOf('=');
                if (idx <= 0) return;
                String k = line.substring(0, idx).trim();
                String v = line.substring(idx + 1);
                switch (k) {
                    case "backup_type" -> vo.setBackupType(v);
                    case "backup_file" -> vo.setBackupFile(v);
                    case "backup_size" -> {
                        try { vo.setBackupSize(Long.parseLong(v.trim())); } catch (NumberFormatException ignore) {}
                    }
                    case "backup_md5" -> vo.setBackupMd5(v);
                    case "backup_time" -> vo.setBackupTime(v);
                    case "database" -> vo.setDatabase(v);
                    case "host" -> vo.setHost(v);
                    default -> { /* ignore */ }
                }
            });
        } catch (IOException e) {
            log.warn("读取 META.txt 失败: {}", meta.getAbsolutePath(), e);
            return null;
        }
        vo.setBackupSizeReadable(humanReadable(vo.getBackupSize()));

        // 文件存在性
        File f = new File(dateDir, vo.getBackupFile());
        vo.setVerified(f.isFile() && f.length() > 0);
        return vo;
    }

    private static String humanReadable(long bytes) {
        if (bytes < 1024) return bytes + "B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String prefix = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f%sB", bytes / Math.pow(1024, exp), prefix);
    }

    private static String md5(Path path) throws IOException {
        try {
            java.security.MessageDigest md;
            try {
                md = java.security.MessageDigest.getInstance("MD5");
            } catch (Exception e) {
                throw new IOException("MD5 not available", e);
            }
            try (var is = Files.newInputStream(path)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) md.update(buf, 0, n);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (IOException e) {
            throw e;
        }
    }

    /**
     * 任务执行信息
     */
    public static class TaskInfo {
        private final String taskId;
        private final String dbKind;
        private final String action;
        private volatile String status;
        private volatile Integer exitCode;
        private volatile String output;
        private final long startTime;
        private volatile long finishTime;

        public TaskInfo(String taskId, String dbKind, String action, String status, long startTime) {
            this.taskId = taskId;
            this.dbKind = dbKind;
            this.action = action;
            this.status = status;
            this.startTime = startTime;
        }

        public String getTaskId() { return taskId; }
        public String getDbKind() { return dbKind; }
        public String getAction() { return action; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getExitCode() { return exitCode; }
        public void setExitCode(Integer exitCode) { this.exitCode = exitCode; }
        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }
        public long getStartTime() { return startTime; }
        public long getFinishTime() { return finishTime; }
        public void setFinishTime(long finishTime) { this.finishTime = finishTime; }
    }
}
