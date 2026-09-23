#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BUILD="$ROOT/build"
OUTPUT="$ROOT/output"
rm -rf "$BUILD" "$OUTPUT"
mkdir -p "$BUILD/classes" "$OUTPUT"

find "$ROOT/src" -name '*.java' -print0 | xargs -0 javac -source 8 -target 8 -d "$BUILD/classes"
printf 'Main-Class: com.apkeditor.gui.Main\n' > "$BUILD/manifest.mf"
jar cfm "$OUTPUT/APKEditor-GUI.jar" "$BUILD/manifest.mf" \
	-C "$BUILD/classes" . -C "$ROOT/example/cli comands" .
printf 'Created %s\n' "$OUTPUT/APKEditor-GUI.jar"