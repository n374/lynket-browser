#!/usr/bin/env bash
#
# RAS-55 · SPIKE-ONLY 取证脚本（design §6.1 / spec 三条通过标准）
#
# 逐「Android 版本 × 浏览器版本」跑一格：先用独立浏览器直开探针页取基线令牌，再用原生气泡打开同一 URL，
# 抓「完整」dumpsys（activities/top/window containers/bubbles）+ logcat(BUBBLE_PROBE|BUBBLE_SPIKE) + 截图，
# 交叉断言「气泡 task 内 top Activity == 被测浏览器 CCT」。本脚本只负责可复现取证与 fail-loud 提示，
# 不代替人工判读；不随产品发布。
#
# 用法：
#   BROWSER_PKG=com.android.chrome PORT=8000 ./capture.sh <tag>
#     <tag> 建议写成 "<android版本>_<浏览器版本>_targetsdk29"，作为产物文件名后缀。
#
# 前置（脚本会尽力校验，但最终以人工确认为准）：
#   - adb 连到目标设备/模拟器；被测浏览器已装、版本已记录；用普通 profile、非隐身、未启用 ephemeral CCT。
#   - 探针页由 `python3 -m http.server $PORT`（在仓库 assets 目录）托管；模拟器内经 http://10.0.2.2:$PORT 可达。
#   - Lynket DEBUG 构建已安装，且 customTabPackage 偏好已设为 $BROWSER_PKG（否则气泡会打开别的浏览器）。
set -uo pipefail

TAG="${1:?用法: BROWSER_PKG=... PORT=... ./capture.sh <tag>}"
BROWSER_PKG="${BROWSER_PKG:?需指定 BROWSER_PKG=被测浏览器包名}"
PORT="${PORT:-8000}"
LYNKET_PKG="${LYNKET_PKG:-arun.com.chromer}"
PROBE_URL="http://10.0.2.2:${PORT}/login-state-probe.html"
OUT="${OUT_DIR:-./capture_${TAG}}"
TRIGGER=".bubbles.system.BubbleSpikeTriggerActivity"

mkdir -p "$OUT"
echo "== RAS-55 capture: tag=$TAG browser=$BROWSER_PKG url=$PROBE_URL out=$OUT =="

adb get-state >/dev/null 2>&1 || { echo "🔴 adb 无设备连接"; exit 1; }

# 记录环境事实（矩阵每格必留：Android 版本 / 浏览器版本 / targetSdk / 包名）
{
  echo "tag=$TAG"
  echo "capturedAtDeviceTime=$(adb shell date -u '+%Y-%m-%dT%H:%M:%SZ' 2>/dev/null)"
  echo "android.release=$(adb shell getprop ro.build.version.release 2>/dev/null)"
  echo "android.sdk=$(adb shell getprop ro.build.version.sdk 2>/dev/null)"
  echo "device=$(adb shell getprop ro.product.model 2>/dev/null)"
  echo "browserPkg=$BROWSER_PKG"
  echo "browserVersion=$(adb shell dumpsys package "$BROWSER_PKG" 2>/dev/null | grep -m1 versionName)"
  echo "lynketPkg=$LYNKET_PKG"
  echo "probeUrl=$PROBE_URL"
} | tee "$OUT/env.txt"

# ── 真机执行学到的关键前置（2026-07-16 实测沉淀，缺一不可）────────────────────────
# 1) 原生气泡必须显式放行，否则通知只会降级成普通通知（dumpsys 里 isBubble=false）：
adb shell settings put secure notification_bubbles 1
adb shell cmd notification set_bubbles "$LYNKET_PKG" 1
# 注意：channel 在首次 showBubbles 时才创建。若 set_bubbles_channel 报 "null object"，
# 说明 channel 还没建——先触发一次气泡（下方 [3]）再执行本行，然后重触发一次即变气泡：
adb shell cmd notification set_bubbles_channel "$LYNKET_PKG" BUBBLE_NOTIFICATION_CHANNEL_ID_v2 true 2>/dev/null || true
# 2) 令牌取证走 http.server 的 access log beacon，而非 logcat：
#    稳定版 Chrome 不把页面 console.log 转发到 logcat，故 logcat 抓不到 [BUBBLE_PROBE]；
#    探针页已改为额外发 GET /beacon?src=..&ck=..&ls=..&wv=..，令牌落在托管服务器 access log。
#    → 独立浏览器直开用 ?src=baseline，气泡打开用 ?src=bubble，从 server log 读并比对两者令牌。
# 3) 气泡默认不自动展开（setAutoExpandBubble(false)），必须点开：脚本 adb input tap 命中气泡圆标
#    （静止在屏幕左/右边缘）；首次点击可能只是关掉 "Chat using bubbles" 教学气泡，需再点一次。
# ─────────────────────────────────────────────────────────────────────────────

echo "-- [1/6] 清被测浏览器站点数据（从基线起测；勿清错包）--"
adb shell pm clear "$BROWSER_PKG" >/dev/null
adb logcat -c

echo "-- [2/6] 独立浏览器直开探针页（显式 -p，回读 resolved activity）--"
adb shell am start -a android.intent.action.VIEW -p "$BROWSER_PKG" -d "$PROBE_URL" \
  | tee "$OUT/baseline_am_start_${TAG}.txt"
echo "   ⏳ 等页面加载 + 令牌落盘..."; sleep 6
adb logcat -d | grep -a "BUBBLE_PROBE" | tee "$OUT/baseline_probe_${TAG}.txt"
adb exec-out screencap -p > "$OUT/baseline_shot_${TAG}.png"
# 回到桌面，避免独立浏览器残留影响气泡形态判读
adb shell input keyboard event KEYCODE_HOME >/dev/null 2>&1 || adb shell input keyevent KEYCODE_HOME
sleep 1

echo "-- [3/6] 触发原生气泡打开同一 URL（experiment: 外部浏览器 CCT 薄壳）--"
adb logcat -c
adb shell am start -n "${LYNKET_PKG}/${TRIGGER}" -e url "$PROBE_URL" -e target experiment \
  | tee "$OUT/bubble_am_start_${TAG}.txt"
echo "   ⏳ 等气泡出现——请在设备上点开气泡使其 expanded（脚本无法代点）..."; sleep 8

echo "-- [4/6] 决定性引擎证据：保存【完整】dumpsys 原文（勿 grep -A3，会丢 taskId/windowingMode/栈）--"
adb shell dumpsys activity activities > "$OUT/dump_activities_${TAG}.txt"
adb shell dumpsys activity top        > "$OUT/dump_top_${TAG}.txt"
adb shell dumpsys window containers   > "$OUT/dump_window_${TAG}.txt"
adb shell dumpsys activity bubbles    > "$OUT/dump_bubbles_${TAG}.txt" 2>/dev/null

echo "-- [5/6] 辅助信号 + 存储令牌 + spike 实际 flags --"
adb logcat -d | grep -aE "BUBBLE_PROBE|BUBBLE_SPIKE" | tee "$OUT/bubble_logcat_${TAG}.txt"
adb exec-out screencap -p > "$OUT/bubble_shot_${TAG}.png"

echo "-- [6/6] 脚本化断言（辅助，仍需人工核完整 dumpsys）--"
{
  echo "### 断言输出（fail loud）——最终判读以完整 dumpsys 原文为准"
  if grep -qa "$BROWSER_PKG" "$OUT/dump_top_${TAG}.txt" "$OUT/dump_activities_${TAG}.txt"; then
    echo "🟢 dumpsys 出现被测浏览器包 $BROWSER_PKG —— 需人工确认它是气泡 task 内的 top/resumed CCT Activity"
  else
    echo "🔴 dumpsys 未见被测浏览器包 $BROWSER_PKG —— 疑似塌全屏/被内置 WebView 承载/CCT 未起（失败态）"
  fi
  if grep -qa "EmbeddableWebViewActivity\|WebViewActivity" "$OUT/dump_top_${TAG}.txt"; then
    echo "🔴 dumpsys top 仍是 Lynket 内置 WebView Activity —— 未走外部 CCT（失败态）"
  fi
  if grep -qa "NO fallback\|launch failed" "$OUT/bubble_logcat_${TAG}.txt"; then
    echo "🔴 [BUBBLE_SPIKE] 打出失败/无 fallback —— CCT 未成功拉起（失败态）"
  fi
  echo "—— 存储共享请人工对比 baseline vs bubble 两条 [BUBBLE_PROBE] 的 cookieToken / localStorageToken（各自严格相等，cookie 须复读 OK）"
} | tee "$OUT/assert_${TAG}.txt"

echo "== 完成。产物在 $OUT/ ；按 spec 三条（浮窗形态 / dumpsys 引擎 / 存储共享）逐条判读并归档矩阵。=="
