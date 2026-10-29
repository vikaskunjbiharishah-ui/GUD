#!/bin/sh
# Gradle recipe for this 4 GB host. Stop leftover daemons first.
# Never run this while the emulator is up: together they exhaust RAM.
set -eu
if [ -x /opt/android-sdk/platform-tools/adb ]; then
  if /opt/android-sdk/platform-tools/adb devices 2>/dev/null | grep -q emulator; then
    echo "REFUSE: emulator is up" >&2
    exit 2
  fi
fi
/tmp/gradle-8.13/bin/gradle --stop >/dev/null 2>&1 || true
for p in /proc/[0-9]*; do
  cmd=$(tr '\0' ' ' < "$p/cmdline" 2>/dev/null || true)
  case "$cmd" in
    *GradleDaemon*|*kotlin-daemon*|*GradleMain*)
      kill "$(basename "$p")" 2>/dev/null || true
      ;;
  esac
done
sleep 1
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/root/.gradle}"
export GRADLE_OPTS="${GRADLE_OPTS:--Xmx384m}"
exec /tmp/gradle-8.13/bin/gradle -p /tmp/GUD/android-native \
  --offline --no-daemon --max-workers=2 \
  -Dorg.gradle.jvmargs="-Xmx1280m -XX:MaxMetaspaceSize=320m -Dfile.encoding=UTF-8" \
  -Dkotlin.daemon.jvmargs="-Xmx512m" \
  "$@"
