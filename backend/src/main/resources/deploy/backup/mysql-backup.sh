#!/bin/bash
###############################################################################
# 智慧粮库 - MySQL 数据库定时备份脚本
# 功能: 全量备份(mysqldump) + 增量备份(binlog) + 自动清理 + 完整性校验
# 用法: mysql-backup.sh [full|incremental|cleanup]
# 默认: full
###############################################################################
set -euo pipefail

# ===== 配置 =====
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-123456}"
DB_NAME="${DB_NAME:-smart_grain}"

# 备份根目录（按需调整）
BACKUP_ROOT="${BACKUP_ROOT:-/data/backups/mysql}"
# binlog 源目录（MySQL 配置 log_bin 时产生）
MYSQL_BINLOG_DIR="${MYSQL_BINLOG_DIR:-/var/lib/mysql}"
# binlog 索引文件
MYSQL_BINLOG_INDEX="${MYSQL_BINLOG_INDEX:-${MYSQL_BINLOG_DIR}/mysql-bin.index}"

# 保留策略
KEEP_DAILY=7        # 日备份保留 7 天
KEEP_WEEKLY=4       # 周备份保留 4 周
KEEP_MONTHLY=6      # 月备份保留 6 个月
KEEP_BINLOG=3       # binlog 保留 3 天

# ===== 颜色与日志 =====
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[0;33m'; NC='\033[0m'
log()  { echo -e "[$(date '+%F %T')] $1"; }
ok()   { echo -e "${GREEN}[$(date '+%F %T')] [OK] $1${NC}"; }
warn() { echo -e "${YELLOW}[$(date '+%F %T')] [WARN] $1${NC}"; }
err()  { echo -e "${RED}[$(date '+%F %T')] [ERR] $1${NC}" >&2; }

# ===== 工具函数 =====
ts() { date '+%Y%m%d_%H%M%S'; }
today() { date '+%Y%m%d'; }
weekday() { date '+%u'; }     # 1=Mon ... 7=Sun
monthday() { date '+%d'; }

ensure_dir() { mkdir -p "$1"; }

# 校验文件存在且非空
check_file() {
    local f="$1"
    [[ -f "$f" ]] || { err "文件不存在: $f"; return 1; }
    [[ -s "$f" ]] || { err "文件为空: $f"; return 1; }
    return 0
}

# 计算 md5
md5sum_file() {
    if command -v md5sum >/dev/null 2>&1; then
        md5sum "$1" | awk '{print $1}'
    else
        md5 -q "$1"
    fi
}

# 备份元数据写入
write_meta() {
    local dir="$1" type="$2" file="$3" size="$4" md5="$5" extra="${6:-}"
    local meta="${dir}/META.txt"
    {
        echo "backup_type=$type"
        echo "backup_file=$file"
        echo "backup_size=$size"
        echo "backup_md5=$md5"
        echo "backup_time=$(date '+%F %T')"
        echo "database=$DB_NAME"
        echo "host=$DB_HOST"
        echo "extra=$extra"
    } > "$meta"
    ok "元数据写入: $meta"
}

# ===== 全量备份 =====
do_full_backup() {
    local backup_type="full"
    local day; day=$(today)
    local wd; wd=$(weekday)
    local md; md=$(monthday)

    # 区分日/周/月备份目录
    local category="daily"
    [[ "$wd" == "7" ]] && category="weekly"
    [[ "$md" == "01" ]] && category="monthly"

    local target_dir="${BACKUP_ROOT}/${category}/${day}"
    ensure_dir "$target_dir"

    local filename="${DB_NAME}_full_${day}.sql.gz"
    local filepath="${target_dir}/${filename}"
    local tmpts="${target_dir}/.tmp_${filename}"

    log "开始 MySQL 全量备份 [${category}] -> ${filepath}"

    # mysqldump 关键参数:
    # --single-transaction: InnoDB 一致性快照,不锁表
    # --routines: 包含存储过程/函数
    # --triggers: 包含触发器
    # --events: 包含事件
    # --set-gtid-purged=OFF: 避免 GTID 冲突
    # --master-data=2: 记录 binlog 位置(注释形式),便于增量恢复
    mysqldump \
        --host="$DB_HOST" --port="$DB_PORT" \
        --user="$DB_USER" --password="$DB_PASSWORD" \
        --single-transaction \
        --routines --triggers --events \
        --set-gtid-purged=OFF \
        --master-data=2 \
        --default-character-set=utf8mb4 \
        --databases "$DB_NAME" 2>"${target_dir}/.dump_error.log" \
        | gzip -9 > "$tmpts"

    check_file "$tmpts" || { err "mysqldump 失败,详见 ${target_dir}/.dump_error.log"; return 1; }
    mv "$tmpts" "$filepath"

    local size; size=$(stat -c%s "$filepath" 2>/dev/null || stat -f%z "$filepath")
    local md5; md5=$(md5sum_file "$filepath")

    # 写元数据
    write_meta "$target_dir" "$backup_type" "$filename" "$size" "$md5" "category=${category}"

    # 记录 binlog 位点（从 dump 文件头部解析）
    local binlog_pos
    binlog_pos=$(zcat "$filepath" 2>/dev/null | grep -m1 "MASTER_LOG_FILE\|CHANGE MASTER" || echo "N/A")
    log "binlog 位点: ${binlog_pos}"

    ok "全量备份完成: ${filepath} (size=$(numfmt --to=iec ${size} 2>/dev/null || echo ${size})B, md5=${md5:0:16}...)"
}

# ===== 增量备份（基于 binlog）=====
do_incremental_backup() {
    local target_dir="${BACKUP_ROOT}/binlog/$(today)"
    ensure_dir "$target_dir"

    [[ -f "$MYSQL_BINLOG_INDEX" ]] || { warn "未找到 binlog 索引: $MYSQL_BINLOG_INDEX,跳过增量备份"; return 0; }

    log "开始 MySQL binlog 增量备份 -> ${target_dir}"

    local count=0
    while read -r binlog; do
        binlog=$(basename "$binlog")
        local src="${MYSQL_BINLOG_DIR}/${binlog}"
        local dst="${target_dir}/${binlog}"

        [[ -f "$src" ]] || continue
        # 跳过正在写入的最后一个 binlog（避免复制到不完整数据）
        # 这里采用 flush logs 后再备份
        if [[ "$count" -eq 0 ]]; then
            mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" -e "FLUSH LOGS;" 2>/dev/null || true
        fi
        count=$((count+1))

        cp -p "$src" "$dst" 2>/dev/null || { warn "复制 binlog 失败: $binlog"; continue; }

        local md5; md5=$(md5sum_file "$dst")
        echo "${binlog} ${md5}" >> "${target_dir}/BINLOG_INDEX.txt"
    done < "$MYSQL_BINLOG_INDEX"

    ok "增量备份完成: ${target_dir} (${count} 个 binlog 文件)"
}

# ===== 自动清理 =====
do_cleanup() {
    log "开始清理过期备份..."

    # 清理 daily: 保留最近 KEEP_DAILY 天
    find "${BACKUP_ROOT}/daily" -mindepth 1 -maxdepth 1 -type d -mtime +${KEEP_DAILY} -exec rm -rf {} \; 2>/dev/null && \
        ok "已清理 ${KEEP_DAILY} 天前的 daily 备份" || warn "daily 清理跳过"

    # 清理 weekly: 保留最近 KEEP_WEEKLY*7 天
    find "${BACKUP_ROOT}/weekly" -mindepth 1 -maxdepth 1 -type d -mtime +$((KEEP_WEEKLY*7)) -exec rm -rf {} \; 2>/dev/null && \
        ok "已清理 ${KEEP_WEEKLY} 周前的 weekly 备份" || warn "weekly 清理跳过"

    # 清理 monthly: 保留最近 KEEP_MONTHLY*30 天
    find "${BACKUP_ROOT}/monthly" -mindepth 1 -maxdepth 1 -type d -mtime +$((KEEP_MONTHLY*30)) -exec rm -rf {} \; 2>/dev/null && \
        ok "已清理 ${KEEP_MONTHLY} 月前的 monthly 备份" || warn "monthly 清理跳过"

    # 清理 binlog: 保留最近 KEEP_BINLOG 天
    find "${BACKUP_ROOT}/binlog" -mindepth 1 -maxdepth 1 -type d -mtime +${KEEP_BINLOG} -exec rm -rf {} \; 2>/dev/null && \
        ok "已清理 ${KEEP_BINLOG} 天前的 binlog 备份" || warn "binlog 清理跳过"

    ok "清理完成"
}

# ===== 完整性校验 =====
do_verify() {
    local target="${1:-${BACKUP_ROOT}/daily/$(today)}"
    log "开始校验备份目录: ${target}"

    [[ -d "$target" ]] || { err "目录不存在: $target"; return 1; }
    [[ -f "${target}/META.txt" ]] || { err "缺少 META.txt"; return 1; }

    local file md5_stored md5_actual
    file=$(grep '^backup_file=' "${target}/META.txt" | cut -d= -f2-)
    md5_stored=$(grep '^backup_md5=' "${target}/META.txt" | cut -d= -f2-)

    check_file "${target}/${file}" || return 1
    md5_actual=$(md5sum_file "${target}/${file}")

    if [[ "$md5_stored" == "$md5_actual" ]]; then
        ok "MD5 校验通过: ${file}"
        # 尝试 gzip 完整性测试
        if gzip -t "${target}/${file}" 2>/dev/null; then
            ok "gzip 完整性测试通过"
        else
            warn "gzip 完整性测试失败（可能是非 gzip 格式）"
        fi
        # 尝试解析 SQL（前 5 行）
        local head_check
        head_check=$(zcat "${target}/${file}" 2>/dev/null | head -5 || echo "")
        [[ "$head_check" == *"-- MySQL dump"* ]] && ok "SQL 头部校验通过" || warn "SQL 头部特征不匹配"
        return 0
    else
        err "MD5 校验失败! stored=${md5_stored} actual=${md5_actual}"
        return 1
    fi
}

# ===== 入口 =====
main() {
    local action="${1:-full}"
    ensure_dir "$BACKUP_ROOT"

    case "$action" in
        full)        do_full_backup ;;
        incremental) do_incremental_backup ;;
        cleanup)     do_cleanup ;;
        verify)      do_verify "${2:-}" ;;
        all)
            do_full_backup
            do_incremental_backup
            do_cleanup
            do_verify
            ;;
        *)
            echo "用法: $0 {full|incremental|cleanup|verify|all}"
            exit 1
            ;;
    esac
}

main "$@"
