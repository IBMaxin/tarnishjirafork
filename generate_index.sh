#!/usr/bin/env bash
# Regenerate code_index.json when source files change.
# Run manually or via Gradle task 'refreshCodeIndex'.
set -euo pipefail
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
OUTFILE="$PROJECT_ROOT/code_index.json"
echo "Indexing source files..."

# Collect all source file paths
{
  echo "["
  first=true
  find "$PROJECT_ROOT" -type f \( \
    -name "*.java" -o -name "*.kt" -o -name "*.kts" -o -name "*.py" \) \
    ! -path "*/build/*" ! -path "*/.git/*" ! -path "*/.gradle/*" \
    -print0 | while IFS= read -r -d '' f; do
      rel="${f#$PROJECT_ROOT/}"
      ext="${rel##*.}"
      classname="$(basename "$rel" ".$ext")"
      [ "$first" = true ] || printf ","
      first=false
      printf '\n  {"path": "%s", "type": "%s", "summary": "%s"}' "$rel" "$ext" "$classname"
    done
  echo ""
  echo "]"
} > "$OUTFILE"

echo "Done: $(python3 -c "import json; print(len(json.load(open('$OUTFILE'))))") entries in code_index.json"
