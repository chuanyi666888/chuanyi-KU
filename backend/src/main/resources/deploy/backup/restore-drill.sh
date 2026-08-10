#!/bin/bash
###############################################################################
# 智慧粮库 - 灾备恢复演练脚本
# 功能: 在隔离环境验证备份可恢复性，含完整性校验 + 业务数据抽样核对
# 用法: restore-drill.sh [mysql|kingbase] [YYYYMMDD]
# 默认: mysql today
###############################################################################
set -euo pipefail

# ===== 配置 =====
DB_TYPE="${1:-mysql}"
BACKUP_DATE="${2:-$(date '+%Y%m%d')}"

# 演练用的临时数据库（避免污染生产）
DRILL_DB_HOST="${DRILL_DB_HOST:-127.0.0.1}"
DRILL_DB_PORT="${DRILL_DB_PORT:-3307}"           # 使用非生产端口
DRILL_DB_USER="${DRILL_DB_USER:-root}"
DRILL_DB_PASSWORD="${DRILL_DB_PASSWORD:-123456}"
DRILL_DB_NAME="${DRILL_DB_NAME:-smart_grain_drill}"

# KingbaseES 演练库
DRILL_KB_HOST="${DRILL_KB_HOST:-127.0.0.1}"
DRILL_KB_PORT="${DRILL_KB_PORT:-54322}"
DRILL_KB_USER="${DRILL_KB_USER:-smartgrain}"
DRILL_KB_PASSWORD="${DRILL_KB_PASSWORD:-smartgrain}"
DRILL_KB_DB="${DRILL_KB_DB:-smart_grain_drill}"
KB_HOME="${KB_HOME:-/opt/kingbase8}"
KB_BIN="${KB_HOME}/bin"

# 备份源
MYSQL_BACKUP_ROOT="${MYSQL_BACKUP_ROOT:-/data/backups/mysql}"
KB_BACKUP_ROOT="${KB_BACKUP_ROOT:-/data/backups/kingbase}"

# 演练报告
DRILL_REPORT_DIR="${DRILL_REPORT_DIR:-/var/log/smartgrain/drill}"
DRILL_REPORT="${DRILL_REPORT_DIR}/drill_${DB_TYPE}_${BACKUP_DATE}_$(date '+%H%M%S').log"

# ===== 日志 =====
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[0;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log()  { echo -e "[$(date '+%F %T')] $1" | tee -a "$DRILL_REPORT"; }
ok()   { echo -e "${GREEN}[$(date '+%F %T')] [OK] $1${NC}"   | tee -a "$DRILL_REPORT"; }
warn() { echo -e "${YELLOW}[$(date '+%F %T')] [WARN] $1${NC}" | tee -a "$DRILL_REPORT"; }
err()  { echo -e "${RED}[$(date '+%F %T')] [ERR] $1${NC}"  | tee -a "$DRILL_REPORT" >&2; }
step() { echo -e "${BLUE}[$(date '+%F %T')] === $1 ===${NC}" | tee -a "$DRILL_REPORT"; }

# 计数器
PASS=0; FAIL=0; WARN=0
pass_check() { ok "✓ $1"; PASS=$((PASS+1)); }
fail_check() { err "✗ $1"; FAIL=$((FAIL+1)); }
warn_check() { warn "? $1"; WARN=$((WARN+1)); }

ensure_dir() { mkdir -p "$1"; }
today() { date '+%Y%m%d'; }

# ===== 演练前置检查 =====
preflight() {
    step "Step 1: 前置检查"
    ensure_dir "$DRILL_REPORT_DIR"
    : > "$DRILL_REPORT"
    log "演练类型: ${DB_TYPE}, 备份日期: ${BACKUP_DATE}"
    log "报告文件: ${DRILL_REPORT}"

    # 命令检查
    case "$DB_TYPE" in
        mysql)
            for cmd in mysql mysqldump gzip zcat; do
                command -v $cmd >/dev/null 2>&1 && pass_check "命令可用: $cmd" || fail_check "命令缺失: $cmd"
            done
            ;;
        kingbase)
            for cmd in ${KB_BIN}/sys_restore ${KB_BIN}/kb_isready gzip; do
                [[ -x "$cmd" ]] && pass_check "命令可用: $cmd" || fail_check "命令缺失: $cmd"
            done
            ;;
    esac
}

# ===== MySQL 恢复演练 =====
drill_mysql() {
    step "Step 2: 定位 MySQL 备份文件"

    # 优先顺序: daily -> weekly -> monthly
    local found=""
    for cat in daily weekly monthly; do
        local dir="${MYSQL_BACKUP_ROOT}/${cat}/${BACKUP_DATE}"
        if [[ -d "$dir" && -f "${dir}/META.txt" ]]; then
            found="$dir"
            log "找到备份: ${dir} (category=${cat})"
            break
        fi
    done
    [[ -n "$found" ]] || { fail_check "未找到 ${BACKUP_DATE} 的 MySQL 备份"; return 1; }

    # 解析 META
    local file md5_stored
    file=$(grep '^backup_file=' "${found}/META.txt" | cut -d= -f2-)
    md5_stored=$(grep '^backup_md5=' "${found}/META.txt" | cut -d= -f2-)
    log "备份文件: ${file}"
    log "记录 MD5: ${md5_stored}"

    step "Step 3: 备份文件完整性校验"
    local filepath="${found}/${file}"
    [[ -f "$filepath" ]] && pass_check "备份文件存在" || { fail_check "备份文件不存在"; return 1; }

    # 文件大小
    local size; size=$(stat -c%s "$filepath" 2>/dev/null || stat -f%z "$filepath")
    log "文件大小: $(numfmt --to=iec ${size} 2>/dev/null || echo ${size})B"
    [[ "$size" -gt 1024 ]] && pass_check "文件大小合理" || fail_check "文件过小(<1KB),疑似异常"

    # MD5 校验
    local md5_actual; md5_actual=$(md5sum "$filepath" 2>/dev/null | awk '{print $1}' || md5 -q "$filepath")
    [[ "$md5_stored" == "$md5_actual" ]] && pass_check "MD5 校验通过" || fail_check "MD5 校验失败 stored=${md5_stored} actual=${md5_actual}"

    # gzip 完整性
    if gzip -t "$filepath" 2>/dev/null; then pass_check "gzip 完整性测试通过"; else fail_check "gzip 完整性测试失败"; fi

    step "Step 4: 准备演练数据库"
    # 删除演练库（如存在）并重建
    if mysql -h"$DRILL_DB_HOST" -P"$DRILL_DB_PORT" -u"$DRILL_DB_USER" -p"$DRILL_DB_PASSWORD" \
        -e "DROP DATABASE IF EXISTS ${DRILL_DB_NAME}; CREATE DATABASE ${DRILL_DB_NAME} DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null; then
        pass_check "演练数据库已就绪: ${DRILL_DB_NAME}"
    else
        fail_check "演练数据库创建失败（确认 ${DRILL_DB_HOST}:${DRILL_DB_PORT} 可用）"
        return 1
    fi

    step "Step 5: 执行数据恢复"
    local restore_start; restore_start=$(date +%s)

    if zcat "$filepath" | mysql -h"$DRILL_DB_HOST" -P"$DRILL_DB_PORT" -u"$DRILL_DB_USER" -p"$DRILL_DB_PASSWORD" "$DRILL_DB_NAME" 2>"${DRILL_REPORT_DIR}/restore_err.log"; then
        pass_check "数据恢复成功"
    else
        fail_check "数据恢复失败,详见 ${DRILL_REPORT_DIR}/restore_err.log"
        return 1
    fi

    local restore_end; restore_end=$(date +%s)
    log "恢复耗时: $((restore_end - restore_start)) 秒"

    step "Step 6: 业务数据抽样核对"
    # 表数量
    local table_count
    table_count=$(mysql -h"$DRILL_DB_HOST" -P"$DRILL_DB_PORT" -u"$DRILL_DB_USER" -p"$DRILL_DB_PASSWORD" \
        -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DRILL_DB_NAME}' AND table_type='BASE TABLE';" 2>/dev/null)
    log "表数量: ${table_count}"
    [[ "$table_count" -ge 10 ]] && pass_check "表数量合理(>=10)" || warn_check "表数量偏少(<10)"

    # 关键业务表抽样
    local tables_to_check=("sys_user" "sys_role" "grain_batch" "inventory" "iot_sensor" "alert_record")
    for t in "${tables_to_check[@]}"; do
        local cnt
        cnt=$(mysql -h"$DRILL_DB_HOST" -P"$DRILL_DB_PORT" -u"$DRILL_DB_USER" -p"$DRILL_DB_PASSWORD" \
            -N -e "SELECT COUNT(*) FROM ${DRILL_DB_NAME}.${t};" 2>/dev/null || echo "ERR")
        if [[ "$cnt" == "ERR" ]]; then
            warn_check "表 ${t} 查询失败（可能不存在）"
        else
            log "  - ${t}: ${cnt} 行"
            [[ "$cnt" -ge 0 ]] && pass_check "表 ${t} 可读 (${cnt} 行)"
        fi
    done

    # 管理员账户可读性
    local admin_count
    admin_count=$(mysql -h"$DRILL_DB_HOST" -P"$DRILL_DB_PORT" -u"$DRILL_DB_USER" -p"$DRILL_DB_PASSWORD" \
        -N -e "SELECT COUNT(*) FROM ${DRILL_DB_NAME}.sys_user WHERE username='admin';" 2>/dev/null || echo "0")
    [[ "$admin_count" -ge 1 ]] && pass_check "管理员账户存在" || fail_check "管理员账户缺失"

    # 库存数据合理性（current_tons >= 0）
    local neg_count
    neg_count=$(mysql -h"$DRILL_DB_HOST" -P"$DRILL_DB_PORT" -u"$DRILL_DB_USER" -p"$DRILL_DB_PASSWORD" \
        -N -e "SELECT COUNT(*) FROM ${DRILL_DB_NAME}.inventory WHERE current_tons < 0;" 2>/dev/null || echo "0")
    [[ "$neg_count" -eq 0 ]] && pass_check "库存数据无负值" || fail_check "库存存在负值: ${neg_count} 条"

    step "Step 7: 清理演练环境"
    if mysql -h"$DRILL_DB_HOST" -P"$DRILL_DB_PORT" -u"$DRILL_DB_USER" -p"$DRILL_DB_PASSWORD" \
        -e "DROP DATABASE IF EXISTS ${DRILL_DB_NAME};" 2>/dev/null; then
        pass_check "演练数据库已清理"
    else
        warn_check "演练数据库清理失败（请手动删除）"
    fi
}

# ===== KingbaseES 恢复演练 =====
drill_kingbase() {
    export PGPASSWORD="$DRILL_KB_PASSWORD"

    step "Step 2: 定位 KingbaseES 备份文件"
    local found=""
    for cat in daily weekly monthly; do
        local dir="${KB_BACKUP_ROOT}/${cat}/${BACKUP_DATE}"
        if [[ -d "$dir" && -f "${dir}/META.txt" ]]; then
            found="$dir"; log "找到备份: ${dir} (category=${cat})"; break
        fi
    done
    [[ -n "$found" ]] || { fail_check "未找到 ${BACKUP_DATE} 的 KingbaseES 备份"; return 1; }

    local file md5_stored
    file=$(grep '^backup_file=' "${found}/META.txt" | cut -d= -f2-)
    md5_stored=$(grep '^backup_md5=' "${found}/META.txt" | cut -d= -f2-)

    step "Step 3: 备份文件完整性校验"
    local filepath="${found}/${file}"
    [[ -f "$filepath" ]] && pass_check "备份文件存在" || { fail_check "备份文件不存在"; return 1; }

    local md5_actual; md5_actual=$(md5sum "$filepath" 2>/dev/null | awk '{print $1}' || md5 -q "$filepath")
    [[ "$md5_stored" == "$md5_actual" ]] && pass_check "MD5 校验通过" || fail_check "MD5 校验失败"

    # sys_restore --list 验证
    if ${KB_BIN}/sys_restore -l "$filepath" >/dev/null 2>&1; then
        pass_check "sys_restore 可读取备份"
    else
        fail_check "sys_restore 无法读取备份"
        return 1
    fi

    step "Step 4: 准备演练数据库"
    ${KB_BIN}/ksql -h"$DRILL_KB_HOST" -p"$DRILL_KB_PORT" -U"$DRILL_KB_USER" -d test \
        -c "DROP DATABASE IF EXISTS ${DRILL_KB_DB};" \
        -c "CREATE DATABASE ${DRILL_KB_DB};" 2>/dev/null \
        && pass_check "演练数据库已就绪" || { fail_check "演练库创建失败"; return 1; }

    step "Step 5: 执行数据恢复"
    if ${KB_BIN}/sys_restore -h"$DRILL_KB_HOST" -p"$DRILL_KB_PORT" -U"$DRILL_KB_USER" \
        -d "$DRILL_KB_DB" -v "$filepath" 2>"${DRILL_REPORT_DIR}/restore_err.log"; then
        pass_check "数据恢复成功"
    else
        fail_check "数据恢复失败"
        return 1
    fi

    step "Step 6: 业务数据抽样核对"
    local table_count
    table_count=$(${KB_BIN}/ksql -h"$DRILL_KB_HOST" -p"$DRILL_KB_PORT" -U"$DRILL_KB_USER" -d"$DRILL_KB_DB" -t -c \
        "SELECT count(*) FROM sys_class WHERE relkind='r' AND relname NOT LIKE 'sys_%';" 2>/dev/null || echo 0)
    log "用户表数量: ${table_count}"
    [[ "$table_count" -ge 10 ]] && pass_check "表数量合理" || warn_check "表数量偏少"

    for t in sys_user grain_batch inventory iot_sensor alert_record; do
        local cnt
        cnt=$(${KB_BIN}/ksql -h"$DRILL_KB_HOST" -p"$DRILL_KB_PORT" -U"$DRILL_KB_USER" -d"$DRILL_KB_DB" -t -c \
            "SELECT count(*) FROM ${t};" 2>/dev/null || echo "ERR")
        if [[ "$cnt" == "ERR" ]]; then
            warn_check "表 ${t} 查询失败"
        else
            log "  - ${t}: ${cnt} 行"
            pass_check "表 ${t} 可读"
        fi
    done

    step "Step 7: 清理演练环境"
    ${KB_BIN}/ksql -h"$DRILL_KB_HOST" -p"$DRILL_KB_PORT" -U"$DRILL_KB_USER" -d test \
        -c "DROP DATABASE IF EXISTS ${DRILL_KB_DB};" 2>/dev/null \
        && pass_check "演练数据库已清理" || warn_check "清理失败,请手动处理"
}

# ===== 演练总结 =====
summary() {
    step "演练总结"
    log "通过: ${PASS}  失败: ${FAIL}  警告: ${WARN}"
    if [[ "$FAIL" -eq 0 ]]; then
        ok "✓ 恢复演练成功，备份有效"
        log "报告: ${DRILL_REPORT}"
        exit 0
    else
        err "✗ 恢复演练失败，存在 ${FAIL} 项检查未通过"
        err "请检查备份文件和恢复流程，报告: ${DRILL_REPORT}"
        exit 1
    fi
}

# ===== 入口 =====
main() {
    preflight
    case "$DB_TYPE" in
        mysql)    drill_mysql ;;
        kingbase) drill_kingbase ;;
        *) err "未知类型: $DB_TYPE (支持: mysql|kingbase)"; exit 1 ;;
    esac
    summary
}

main "$@"
