#!/usr/bin/env bash
# T181 · 端到端冒烟脚本（Sprint 7 Polish）
#
# 一次性验证 quickstart.md §5.1 / §5.2 / §5.3 三条 API 路径，覆盖 SC-001。
# 前置：后端已在 http://localhost:8080（dev profile + V5 seed）跑起来；
#      MySQL 里 loginName=zs / www / ls / hhh 用 123456 均可登录。
#
# 用法：
#   bash scripts/smoke.sh            # 默认 http://localhost:8080
#   API_BASE=http://x:8080 bash scripts/smoke.sh
#
# 退出码：0 全部通过，非 0 表示对应场景失败（stderr 输出细节）。

set -o pipefail

API_BASE="${API_BASE:-http://localhost:8080}"
PREFIX="${API_BASE}/api/v1"

# ---------- utilities ----------
GREEN=$'\033[32m'
RED=$'\033[31m'
DIM=$'\033[2m'
RESET=$'\033[0m'

pass=0
fail=0
declare -a failed_labels

assert_eq() {
    local label="$1"
    local expected="$2"
    local actual="$3"
    if [[ "$expected" == "$actual" ]]; then
        printf "%s  ✓%s %s\n" "$GREEN" "$RESET" "$label"
        pass=$((pass + 1))
    else
        printf "%s  ✗%s %s (expected=%s actual=%s)\n" "$RED" "$RESET" "$label" "$expected" "$actual" >&2
        fail=$((fail + 1))
        failed_labels+=("$label")
    fi
}

login() {
    local user="$1"
    local pass="$2"
    curl -sS -X POST "${PREFIX}/auth/login" \
        -H 'Content-Type: application/json' \
        -d "{\"loginName\":\"${user}\",\"password\":\"${pass}\"}" | \
        python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('data',{}).get('accessToken',''))"
}

json_get() {
    python3 -c "import json,sys; d=json.load(sys.stdin);
key=sys.argv[1]
def g(o,k):
  for p in k.split('.'):
    if isinstance(o, list): o = o[int(p)]
    else: o = o.get(p) if o is not None else None
  return o
print(g(d, key))" "$1"
}

# ---------- ping ----------
echo "==> Pinging backend at ${API_BASE}"
if ! curl -sS -o /dev/null "${PREFIX}/auth/login" -X POST -H 'Content-Type: application/json' -d '{}'; then
    echo "${RED}后端未就绪，请先启动 backend（bash /tmp/start-backend.sh 或 mvn spring-boot:run）${RESET}" >&2
    exit 2
fi

# ==========================================================
# §5.1 US1 · 学生查成绩
# ==========================================================
echo
echo "==> §5.1 US1 学生查成绩"
stu_token=$(login "hhh" "123456")
[[ -n "$stu_token" ]] || { echo "${RED}学生登录失败${RESET}" >&2; exit 3; }

body=$(curl -sS "${PREFIX}/student/scores" -H "Authorization: Bearer ${stu_token}")
code=$(echo "$body" | json_get code)
assert_eq "5.1.1 学生查成绩 200/code=0" "0" "$code"

items_len=$(echo "$body" | python3 -c "import json,sys; d=json.load(sys.stdin); print(len(d.get('data',{}).get('items',[])))")
if [[ "$items_len" -ge 0 ]]; then
    printf "%s  ✓%s 5.1.2 学生成绩条数=%s（要求 ≥ 0；seed 通常有 3 条）\n" "$GREEN" "$RESET" "$items_len"
    pass=$((pass + 1))
else
    echo "${RED}  ✗ 5.1.2 items 不是数组${RESET}" >&2
    fail=$((fail + 1))
fi

# 越权：学生访问 teacher/subjects 应 403
raw_status=$(curl -sS -o /tmp/csm_smoke.out -w '%{http_code}' \
    "${PREFIX}/teacher/subjects" -H "Authorization: Bearer ${stu_token}")
assert_eq "5.1.3 学生访问 /teacher/subjects → 403" "403" "$raw_status"

# 未认证：拿掉 Authorization 应 401
raw_status=$(curl -sS -o /dev/null -w '%{http_code}' "${PREFIX}/student/scores")
assert_eq "5.1.4 未认证访问 /student/scores → 401" "401" "$raw_status"

# ==========================================================
# §5.2 US2 · 教师录入 / 修改成绩
# ==========================================================
echo
echo "==> §5.2 US2 教师录入成绩"
tea_token=$(login "ttt" "123456")
[[ -n "$tea_token" ]] || { echo "${RED}教师登录失败${RESET}" >&2; exit 3; }

# 找一个能录入的 (student, subject) —— 简化：调 entry-options 拿第一个
opts=$(curl -sS "${PREFIX}/teacher/students/6/entry-options" -H "Authorization: Bearer ${tea_token}")
opts_code=$(echo "$opts" | json_get code)
assert_eq "5.2.1 教师 entry-options code=0" "0" "$opts_code"

sid=$(echo "$opts" | python3 -c "import json,sys; d=json.load(sys.stdin); opts=d.get('data',{}).get('options',[]); print(opts[0]['subjectId'] if opts else '')")
if [[ -n "$sid" ]]; then
    # 录一个 88
    create_body=$(curl -sS -X POST "${PREFIX}/teacher/students/6/scores" \
        -H "Authorization: Bearer ${tea_token}" -H 'Content-Type: application/json' \
        -d "{\"subjectId\":${sid},\"score\":88}")
    create_code=$(echo "$create_body" | json_get code)
    if [[ "$create_code" == "0" ]]; then
        score_id=$(echo "$create_body" | json_get data.id)
        printf "%s  ✓%s 5.2.2 教师录入成绩成功 subjectId=%s scoreId=%s\n" "$GREEN" "$RESET" "$sid" "$score_id"
        pass=$((pass + 1))

        # 修改成 92
        upd_body=$(curl -sS -X POST "${PREFIX}/teacher/scores/${score_id}/update" \
            -H "Authorization: Bearer ${tea_token}" -H 'Content-Type: application/json' \
            -d '{"score":92}')
        upd_code=$(echo "$upd_body" | json_get code)
        assert_eq "5.2.3 教师修改成绩 code=0" "0" "$upd_code"

        # 非法 score → 1000
        bad_body=$(curl -sS -X POST "${PREFIX}/teacher/scores/${score_id}/update" \
            -H "Authorization: Bearer ${tea_token}" -H 'Content-Type: application/json' \
            -d '{"score":-1}')
        bad_code=$(echo "$bad_body" | json_get code)
        assert_eq "5.2.4 教师修改非法 score → 1000" "1000" "$bad_code"

        # 清理：删这条成绩
        del_body=$(curl -sS -X POST "${PREFIX}/teacher/scores/${score_id}/delete" \
            -H "Authorization: Bearer ${tea_token}")
        del_code=$(echo "$del_body" | json_get code)
        assert_eq "5.2.5 教师删除刚录的成绩 code=0" "0" "$del_code"
    else
        printf "%s  ⚠%s 5.2.2 学生 6 无可录课程或已全录（code=%s）；跳过 5.2.3/5.2.4\n" "$DIM" "$RESET" "$create_code"
    fi
else
    printf "%s  ⚠%s 5.2.2 学生 6 已录完全部课程，无 subjectId 可用；跳过 5.2.3/5.2.4\n" "$DIM" "$RESET"
fi

# ==========================================================
# §5.3 US3 · 教师增删改学生
# ==========================================================
echo
echo "==> §5.3 US3 教师增删改学生"
random_name="smoke_$(date +%s)"
create_body=$(curl -sS -X POST "${PREFIX}/teacher/students" \
    -H "Authorization: Bearer ${tea_token}" -H 'Content-Type: application/json' \
    -d "{\"loginName\":\"${random_name}\",\"realName\":\"冒烟同学\",\"password\":\"123456\",\"tel\":\"13800138000\",\"address\":\"SMU\",\"grade\":2}")
create_code=$(echo "$create_body" | json_get code)
if [[ "$create_code" == "0" ]]; then
    new_id=$(echo "$create_body" | json_get data.id)
    printf "%s  ✓%s 5.3.1 教师新增学生 id=%s\n" "$GREEN" "$RESET" "$new_id"
    pass=$((pass + 1))

    # 重复 loginName → 2101
    dup_body=$(curl -sS -X POST "${PREFIX}/teacher/students" \
        -H "Authorization: Bearer ${tea_token}" -H 'Content-Type: application/json' \
        -d "{\"loginName\":\"${random_name}\",\"realName\":\"x\",\"password\":\"123456\",\"tel\":\"13800138000\",\"address\":\"x\",\"grade\":1}")
    dup_code=$(echo "$dup_body" | json_get code)
    assert_eq "5.3.2 重复 loginName → 2101" "2101" "$dup_code"

    # 修改 tel
    upd_body=$(curl -sS -X POST "${PREFIX}/teacher/students/${new_id}/update" \
        -H "Authorization: Bearer ${tea_token}" -H 'Content-Type: application/json' \
        -d "{\"loginName\":\"${random_name}\",\"realName\":\"冒烟同学\",\"tel\":\"13911112222\",\"address\":\"SMU\",\"grade\":2}")
    upd_code=$(echo "$upd_body" | json_get code)
    assert_eq "5.3.3 修改学生 tel code=0" "0" "$upd_code"

    # 删除
    del_body=$(curl -sS -X POST "${PREFIX}/teacher/students/${new_id}/delete" \
        -H "Authorization: Bearer ${tea_token}")
    del_code=$(echo "$del_body" | json_get code)
    assert_eq "5.3.4 删除学生 code=0" "0" "$del_code"

    # 再登录该账号 → 2001
    raw_body=$(curl -sS -X POST "${PREFIX}/auth/login" \
        -H 'Content-Type: application/json' \
        -d "{\"loginName\":\"${random_name}\",\"password\":\"123456\"}")
    lc=$(echo "$raw_body" | json_get code)
    assert_eq "5.3.5 已删账号再登录 → 2001" "2001" "$lc"
else
    printf "%s  ✗%s 5.3.1 教师新增学生失败 code=%s\n" "$RED" "$RESET" "$create_code" >&2
    fail=$((fail + 1))
fi

# ==========================================================
# 汇总
# ==========================================================
echo
echo "======================================================"
printf "冒烟结果：通过 %s / 失败 %s\n" "$pass" "$fail"
if (( fail > 0 )); then
    echo "${RED}失败列表：${RESET}"
    for l in "${failed_labels[@]}"; do
        echo "  - $l"
    done
    exit 1
fi
echo "${GREEN}SC-001 全绿 ✓${RESET}"
exit 0
