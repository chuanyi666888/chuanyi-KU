#!/bin/bash
###############################################################################
# 智慧粮库 - KingbaseES 数据库定时备份脚本
# 国产化适配版本（基于 sys_dump / sys_basebackup）
# 用法: kingbase-backup.sh [full|incremental|cleanup|verify]
###############################################################################
set -euo pipefail

# ===== 配置 =====
KB_HOST="${KB_HOST:-127.0.0.1}"
KB_PORT="${KB_PORT:-54321}"
KB_USER="${KB_USER:-smartgrain}"
KB_PASSWORD="${KB_PASSWORD:-smartgrain}"
KB_DB="${KB_DB:-smart_grain}"
KB_HOME="${KB_HOME:-/opt/kingbase8}"
KB_BIN="${KB_HOME}/bin"
KB_DATA="${KB_DATA:-/opt/kingbase8/data}"

# KingbaseES 使用 sys_dump 需要 PGPASSWORD
export PGPASSWORD="$KB_PASSWORD"

# 备份根目录
BACKUP_ROOT="${BACKUP_ROOT:-/data/backups/kingbase}"

# 保留策略
KEEP_DAILY=7
KEEP_WEEKLY=4
KEEP_MONTHLY=6
KEEP_WAL=3

# ===== 日志 =====
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[0;33m'; NC='\033[0m'
log()  { echo -e "[$(date '+%F %T')] $1"; }
ok()   { echo -e "${GREEN}[$(date '+%F %T')] [OK] $1${NC}"; }
warn() { echo -e "${YELLOW}[$(date '+%F %T')] [WARN] $1${NC}"; }
err()  { echo -e "${RED}[$(date '+%F %T')] [ERR] $1${NC}" >&2; }

today() { date '+%Y%m%d'; }
weekday() { date '+%u'; }
monthday() { date '+%d'; }
ensure_dir() { mkdir -p "$1"; }

check_file() {
    local f="$1"
    [[ -f "$f" ]] || { err "文件不存在: $f"; return 1; }
    [[ -s "$f" ]] || { err "文件为空: $f"; return 1; }
    return 0
}

md5sum_file() {
    if command -v md5sum >/dev/null 2>&1; then md5sum "$1" | awk '{print $1}'
    else md5 -q "$1"; fi
}

write_meta() {
    local dir="$1" type="$2" file="$3" size="$4" md5="$5" extra="${6:-}"
    {
        echo "backup_type=$type"
        echo "backup_file=$file"
        echo "backup_size=$size"
        echo "backup_md5=$md5"
        echo "backup_time=$(date '+%F %T')"
        echo "database=$KB_DB"
        echo "host=$KB_HOST"
        echo "kb_version=$(${KB_BIN}/kb_isready 2>/dev/null || echo unknown)"
        echo "extra=$extra"
    } > "${dir}/META.txt"
    ok "元数据写入: ${dir}/META.txt"
}

# ===== 全量逻辑备份（sys_dump）=====
do_full_backup() {
    local day; day=$(today)
    local wd; wd=$(weekday); local md; md=$(monthday)
    local category="daily"
    [[ "$wd" == "7" ]] && category="weekly"
    [[ "$md" == "01" ]] && category="monthly"

    local target_dir="${BACKUP_ROOT}/${category}/${day}"
    ensure_dir "$target_dir"

    local filename="${KB_DB}_full_${day}.dmp"
    local filepath="${target_dir}/${filename}"

    log "开始 KingbaseES 逻辑全量备份 [${category}] -> ${filepath}"

    # sys_dump 参数:
    # -Fc: 自定义压缩格式
    # -v: 详细输出
    # -h/-p/-U: 连接参数
    ${KB_BIN}/sys_dump \
        -h "$KB_HOST" -p "$KB_PORT" -U "$KB_USER" \
        -d "$KB_DB" -Fc -v \
        -f "$filepath" 2>"${target_dir}/.dump_error.log"

    check_file "$filepath" || { err "sys_dump 失败,详见 ${target_dir}/.dump_error.log"; return 1; }

    local size md5
    size=$(stat -c%s "$filepath" 2>/dev/null || stat -f%z "$filepath")
    md5=$(md5sum_file "$filepath")

    write_meta "$target_dir" "full" "$filename" "$size" "$md5" "category=${category}"

    ok "全量备份完成: ${filepath} (size=$(numfmt --to=iec ${size} 2>/dev/null || echo ${size})B)"
}

# ===== 物理基础备份（sys_basebackup，用于增量恢复基础）=====
do_basebackup() {
    local day; day=$(today)
    local target_dir="${BACKUP_ROOT}/base/${day}"
    ensure_dir "$target_dir"

    log "开始 KingbaseES 物理基础备份 -> ${target_dir}"

    # sys_basebackup 需要 replication 权限
    ${KB_BIN}/sys_basebackup \
        -h "$KB_HOST" -p "$KB_PORT" -U repl \
        -D "$target_dir" -Ft -z -P \
        -X stream -S replica_slot 2>"${target_dir}/.base_error.log" \
        || { warn "sys_basebackup 失败(需配置 replication slot),跳过"; return 0; }

    ok "物理基础备份完成: ${target_dir}"
}

# ===== 增量备份（归档 WAL）=====
do_incremental_backup() {
    local target_dir="${BACKUP_ROOT}/wal/$(today)"
    ensure_dir "$target_dir"

    local archive_dir="${KB_DATA}/archive_wal"
    [[ -d "$archive_dir" ]] || { warn "归档 WAL 目录不存在: $archive_dir,跳过增量备份"; return 0; }

    log "开始 KingbaseES WAL 归档增量备份 -> ${target_dir}"

    local count=0
    for wal in "$archive_dir"/*; do
        [[ -f "$wal" ]] || continue
        local name; name=$(basename "$wal")
        local dst="${target_dir}/${name}"
        [[ -f "$dst" ]] && continue   # 已备份过
        cp -p "$wal" "$dst"
        echo "${name} $(md5sum_file "$dst")" >> "${target_dir}/WAL_INDEX.txt"
        count=$((count+1))
    done

    ok "增量备份完成: ${target_dir} (${count} 个 WAL 文件)"
}

# ===== 自动清理 =====
do_cleanup() {
    log "开始清理过期 KingbaseES 备份..."
    find "${BACKUP_ROOT}/daily"   -mindepth 1 -maxdepth 1 -type d -mtime +${KEEP_DAILY}            -exec rm -rf {} \; 2>/dev/null && ok "daily 清理完成"   || warn "daily 清理跳过"
    find "${BACKUP_ROOT}/weekly"  -mindepth 1 -maxdepth 1 -type d -mtime +$((KEEP_WEEKLY*7))       -exec rm -rf {} \; 2>/dev/null && ok "weekly 清理完成"  || warn "weekly 清理跳过"
    find "${BACKUP_ROOT}/monthly" -mindepth 1 -maxdepth 1 -type d -mtime +$((KEEP_MONTHLY*30))     -exec rm -rf {} \; 2>/dev/null && ok "monthly 清理完成" || warn "monthly 清理跳过"
    find "${BACKUP_ROOT}/wal"     -mindepth 1 -maxdepth 1 -type d -mtime +${KEEP_WAL}              -exec rm -rf {} \; 2>/dev/null && ok "wal 清理完成"     || warn "wal 清理跳过"
    find "${BACKUP_ROOT}/base"    -mindepth 1 -maxdepth 1 -type d -mtime +${KEEP_WEEKLY*7}         -exec rm -rf {} \; 2>/dev/null && ok "base 清理完成"    || warn "base 清理跳过"
    ok "清理完成"
}

# ===== 完整性校验 =====
do_verify() {
    local target="${1:-${BACKUP_ROOT}/daily/$(today)}"
    log "校验 KingbaseES 备份: ${target}"
    [[ -d "$target" ]] || { err "目录不存在"; return 1; }
    [[ -f "${target}/META.txt" ]] || { err "缺少 META.txt"; return 1; }

    local file md5_stored md5_actual
    file=$(grep '^backup_file=' "${target}/META.txt" | cut -d= -f2-)
    md5_stored=$(grep '^backup_md5=' "${target}/META.txt" | cut -d= -f2-)
    check_file "${target}/${file}" || return 1
    md5_actual=$(md5sum_file "${target}/${file}")

    if [[ "$md5_stored" == "$md5_actual" ]]; then
        ok "MD5 校验通过"
        # 尝试 sys_restore --list 验证备份可读
        if ${KB_BIN}/sys_restore -l "${target}/${file}" >/dev/null 2>&1; then
            ok "sys_restore 完整性测试通过"
        else
            warn "sys_restore 完整性测试失败"
        fi
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
        basebackup)  do_basebackup ;;
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
            echo "用法: $0 {full|basebackup|incremental|cleanup|verify|all}"
            exit 1
            ;;
    esac
}

main "$@"
