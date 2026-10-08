#!/bin/sh
# Lance les tests de logique pure d'un module sans passer par Gradle (quelques secondes).
#   ./tests.sh commun      ./tests.sh contacts
# Les tests d'écran (ceux qui utilisent le simulateur Android) passent par
#   ANDROID_HOME=~/android-sdk ./gradlew :contacts:testDebugUnitTest
set -e
cd "$(dirname "$0")"
MODULE="${1:?nom du module}"
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1"
JUNIT=$(find "$CACHE/junit" -name 'junit-4*.jar' | grep -v sources | head -1)
HAMCREST=$(find "$CACHE/org.hamcrest" -name 'hamcrest-core-*.jar' | grep -v sources | head -1)
SORTIE=$(mktemp -d)
trap 'rm -rf "$SORTIE"' EXIT
# La logique pure vit dans les dossiers « logique » ; dans le socle, c'est tout ce qui n'utilise rien d'Android.
SOURCES=$(grep -L 'import android' commun/src/main/java/fr/manboyx/commun/*.java)
TESTS=""
if [ "$MODULE" = commun ]; then
    TESTS=$(find commun/src/test/java -name '*Test.java')
else
    SOURCES="$SOURCES $(find "$MODULE/src/main/java" -path '*/logique/*' -name '*.java')"
    TESTS=$(find "$MODULE/src/test/java" -path '*/logique/*' -name '*Test.java')
fi
# Les tests peuvent avoir des classes d'aide (sans « Test » à la fin) : on compile tout le dossier.
AIDES=$(for t in $TESTS; do dirname "$t"; done | sort -u | while read d; do find "$d" -maxdepth 1 -name '*.java' ! -name '*Test.java'; done)
javac -encoding UTF-8 -d "$SORTIE" -cp "$JUNIT:$HAMCREST" $SOURCES $TESTS $AIDES
java -cp "$SORTIE:$JUNIT:$HAMCREST" org.junit.runner.JUnitCore \
    $(echo "$TESTS" | tr ' ' '\n' | sed 's|.*/src/test/java/||; s|\.java$||; s|/|.|g')
