#!/bin/sh
# Gradle recipe for this 4 GB host.
# Never run this while the emulator is up: together they exhaust RAM.
# A cold daemon often closes the test-worker socket (connection refused)
# before any test. Reuse a live daemon. Set GUD_GRADLE_RESET=1 to stop it.
set -eu
if [ -x /opt/android-sdk/platform-tools/adb ]; then
  if /opt/android-sdk/platform-tools/adb devices 2>/dev/null | grep -q emulator; then
    echo "REFUSE: emulator is up" >&2
    exit 2
  fi
fi
if [ "${GUD_GRADLE_RESET:-}" = "1" ]; then
  /tmp/gradle-8.13/bin/gradle --stop >/dev/null 2>&1 || true
  for p in /proc/[0-9]*; do
    cmd=$(tr '\0' ' ' < "$p/cmdline" 2>/dev/null || true)
    case "$cmd" in
      *GradleDaemon*|*kotlin-daemon*|*GradleMain*|*GradleWorkerMain*)
        kill "$(basename "$p")" 2>/dev/null || true
        ;;
    esac
  done
  sleep 1
fi
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export PATH="$JAVA_HOME/bin:$PATH"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/root/.gradle}"
# LANG=C.UTF-8 leaves user.country and user.variant unset. Gradle then puts a
# valueless -Duser.variant on the test worker. Type tag 71 (ASCII 'G') is a
# discarded handshake on the daemon socket and still prints on a green run.
# preferIPv4Stack keeps the daemon on 127.0.0.1 (localhost is ::1 first).
# Do not pass --no-daemon: with -Dorg.gradle.jvmargs it forks a single-use
# daemon and the worker gets connection refused. Reuse the daemon.
locale_flags="-Djava.net.preferIPv4Stack=true -Duser.country=US -Duser.language=en -Duser.variant=POSIX"
export JAVA_TOOL_OPTIONS="$locale_flags"
export GRADLE_OPTS="-Xmx384m $locale_flags"
workers="--max-workers=2"
for arg in "$@"; do
  case "$arg" in
    --max-workers|--max-workers=*) workers="" ;;
  esac
done
exec /tmp/gradle-8.13/bin/gradle -p /tmp/GUD/android-native \
  --offline $workers \
  -Dorg.gradle.jvmargs="-Xmx1280m -XX:MaxMetaspaceSize=320m -Dfile.encoding=UTF-8 $locale_flags" \
  -Dkotlin.daemon.jvmargs="-Xmx384m $locale_flags" \
  "$@"
