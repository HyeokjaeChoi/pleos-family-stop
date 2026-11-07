# Gradle 9 needs JDK 17+; fall back to Android Studio's bundled JBR on macOS.
jbr="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
[ -z "$JAVA_HOME" ] && [ -d "$jbr" ] && export JAVA_HOME="$jbr"
