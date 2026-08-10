#!/bin/bash
###############################################################################
# 智慧粮库 - 备份文件远程同步脚本（rsync over SSH）
# 功能: 将本地备份目录增量同步到远程备份服务器，支持异地容灾
# 用法: rsync-sync.sh [mysql|kingbase|all|verify]
###############################################################################
set -euo pipefail

# ===== 配置 =====
# 远程备份服务器（请在 ~/.ssh/config 配置免密登录或使用 SSH_KEY）
REMOTE_USER="${REMOTE_USER:-backup}"
REMOTE_HOST="${REMOTE_HOST:-backup.example.com}"
REMOTE_PORT="${REMOTE_PORT:-22}"
SSH_KEY="${SSH_KEY:-/home/smartgrain/.ssh/id_rsa_backup}"

# 各类备份的源/目标
MYSQL_LOCAL_ROOT="${MYSQL_LOCAL_ROOT:-/data/backups/mysql}"
MYSQL_REMOTE_ROOT="${MYSQL_REMOTE_ROOT:-/data/backups/mysql}"

KB_LOCAL_ROOT="${KB_LOCAL_ROOT:-/data/backups/kingbase}"
KB_REMOTE_ROOT="${KB_REMOTE_ROOT:-/data/backups/kingbase}"

# 应用配置/上传文件
APP_LOCAL_ROOT="${APP_LOCAL_ROOT:-/opt/smartgrain/uploads}"
APP_REMOTE_ROOT="${APP_REMOTE_ROOT:-/data/backups/app-uploads}"

# rsync 参数
RSYNC_OPTS="-avz --partial --partial-dir=.rsync-partial \
    --delete --delete-excluded \
    --bwlimit=10240 \
    --timeout=3600 \
    --stats \
    --log-file=/var/log/smartgrain/rsync-$(date '+%Y%m%d').log"

# 带宽限制（KB/s）默认 10MB/s
# --partial: 支持断点续传
# --delete: 保持源/目标一致（注意：会删除远程多余文件）
# --bwlimit: 限速，避免占满带宽
# --timeout: 防止卡死

# ===== 日志 =====
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[0;33m'; NC='\033[0m'
log()  { echo -e "[$(date '+%F %T')] $1"; }
ok()   { echo -e "${GREEN}[$(date '+%F %T')] [OK] $1${NC}"; }
warn() { echo -e "${YELLOW}[$(date '+%F %T')] [WARN] $1${NC}"; }
err()  { echo -e "${RED}[$(date '+%F %T')] [ERR] $1${NC}" >&2; }

ensure_dir() { mkdir -p "$1"; }

# 远程 SSH 命令前缀
ssh_prefix() {
    echo "ssh -p ${REMOTE_PORT} -i ${SSH_KEY} -o StrictHostKeyChecking=no -o ConnectTimeout=10"
}

# 校验 SSH 连通性
check_ssh() {
    log "校验 SSH 连通性: ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PORT}"
    if $(ssh_prefix) "${REMOTE_USER}@${REMOTE_HOST}" "echo ok" >/dev/null 2>&1; then
        ok "SSH 连接正常"
        return 0
    else
        err "SSH 连接失败，请检查密钥 ${SSH_KEY} 与 ${REMOTE_HOST} 配置"
        return 1
    fi
}

# 远程目录初始化
ensure_remote_dir() {
    local dir="$1"
    log "初始化远程目录: ${dir}"
    $(ssh_prefix) "${REMOTE_USER}@${REMOTE_HOST}" "mkdir -p ${dir}" || {
        err "远程目录创建失败: ${dir}"
        return 1
    }
}

# ===== 同步函数 =====
sync_mysql() {
    log "开始同步 MySQL 备份 -> ${REMOTE_HOST}:${MYSQL_REMOTE_ROOT}"
    [[ -d "$MYSQL_LOCAL_ROOT" ]] || { warn "本地 MySQL 备份目录不存在，跳过"; return 0; }
    ensure_remote_dir "$MYSQL_REMOTE_ROOT"

    rsync $RSYNC_OPTS -e "$(ssh_prefix)" \
        "${MYSQL_LOCAL_ROOT}/" "${REMOTE_USER}@${REMOTE_HOST}:${MYSQL_REMOTE_ROOT}/" \
        && ok "MySQL 备份同步完成" \
        || { err "MySQL 备份同步失败"; return 1; }
}

sync_kingbase() {
    log "开始同步 KingbaseES 备份 -> ${REMOTE_HOST}:${KB_REMOTE_ROOT}"
    [[ -d "$KB_LOCAL_ROOT" ]] || { warn "本地 KingbaseES 备份目录不存在，跳过"; return 0; }
    ensure_remote_dir "$KB_REMOTE_ROOT"

    rsync $RSYNC_OPTS -e "$(ssh_prefix)" \
        "${KB_LOCAL_ROOT}/" "${REMOTE_USER}@${REMOTE_HOST}:${KB_REMOTE_ROOT}/" \
        && ok "KingbaseES 备份同步完成" \
        || { err "KingbaseES 备份同步失败"; return 1; }
}

sync_app_files() {
    log "开始同步应用上传文件 -> ${REMOTE_HOST}:${APP_REMOTE_ROOT}"
    [[ -d "$APP_LOCAL_ROOT" ]] || { warn "本地应用文件目录不存在，跳过"; return 0; }
    ensure_remote_dir "$APP_REMOTE_ROOT"

    rsync $RSYNC_OPTS -e "$(ssh_prefix)" \
        "${APP_LOCAL_ROOT}/" "${REMOTE_USER}@${REMOTE_HOST}:${APP_REMOTE_ROOT}/" \
        && ok "应用文件同步完成" \
        || { err "应用文件同步失败"; return 1; }
}

# ===== 校验远程备份 =====
do_verify() {
    log "校验远程备份完整性..."
    local today; today=$(date '+%Y%m%d')

    $(ssh_prefix) "${REMOTE_USER}@${REMOTE_HOST}" bash -s <<EOF
set -e
echo "[远程] 校验 MySQL 备份..."
if [[ -f "${MYSQL_REMOTE_ROOT}/daily/${today}/META.txt" ]]; then
    cat "${MYSQL_REMOTE_ROOT}/daily/${today}/META.txt"
    cd "${MYSQL_REMOTE_ROOT}/daily/${today}"
    file=\$(grep '^backup_file=' META.txt | cut -d= -f2-)
    md5_stored=\$(grep '^backup_md5=' META.txt | cut -d= -f2-)
    md5_actual=\$(md5sum "\$file" | awk '{print \$1}')
    [[ "\$md5_stored" == "\$md5_actual" ]] && echo "[远程] MySQL MD5 校验通过" || echo "[远程] MySQL MD5 校验失败!"
else
    echo "[远程] MySQL 今日备份不存在"
fi

echo "[远程] 校验 KingbaseES 备份..."
if [[ -f "${KB_REMOTE_ROOT}/daily/${today}/META.txt" ]]; then
    cat "${KB_REMOTE_ROOT}/daily/${today}/META.txt"
    cd "${KB_REMOTE_ROOT}/daily/${today}"
    file=\$(grep '^backup_file=' META.txt | cut -d= -f2-)
    md5_stored=\$(grep '^backup_md5=' META.txt | cut -d= -f2-)
    md5_actual=\$(md5sum "\$file" | awk '{print \$1}')
    [[ "\$md5_stored" == "\$md5_actual" ]] && echo "[远程] KingbaseES MD5 校验通过" || echo "[远程] KingbaseES MD5 校验失败!"
else
    echo "[远程] KingbaseES 今日备份不存在"
fi
EOF
    ok "远程校验完成"
}

# ===== 入口 =====
main() {
    ensure_dir /var/log/smartgrain
    local action="${1:-all}"
    check_ssh || exit 1

    case "$action" in
        mysql)     sync_mysql ;;
        kingbase)  sync_kingbase ;;
        app)       sync_app_files ;;
        verify)    do_verify ;;
        all)
            sync_mysql
            sync_kingbase
            sync_app_files
            do_verify
            ;;
        *)
            echo "用法: $0 {mysql|kingbase|app|verify|all}"
            exit 1
            ;;
    esac
}

main "$@"
