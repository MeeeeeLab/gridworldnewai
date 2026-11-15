#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
javac_bin=${JAVA_HOME:+$JAVA_HOME/bin/}javac
jar_bin=${JAVA_HOME:+$JAVA_HOME/bin/}jar
command -v "$javac_bin" >/dev/null || { echo 'Java 21 JDK required; set JAVA_HOME.' >&2; exit 1; }
build=$(mktemp -d)
trap 'rm -rf "$build"' EXIT
lib="$PWD/GridRift-demo (1)/lib"
for module in algorithm game desktop; do
  mkdir -p "$build/$module"
  mapfile -d '' sources < <(find "recovered-src/$module" -name '*.java' -print0)
  compiler_options=()
  [[ -n "${JAVA_HOME:-}" ]] && compiler_options+=("-J-Djava.home=$JAVA_HOME")
  "$javac_bin" "${compiler_options[@]}" --release 21 -cp "$build/algorithm:$build/game:$lib/*" -d "$build/$module" "${sources[@]}"
done
# Keep bundled assets and manifests; replace all recovered classes.
for module in algorithm game desktop; do
  target=$module.jar
  [[ $module == desktop ]] && target=desktop-1.0.0.jar
  cp "$lib/$target" "$build/$target"
  "$jar_bin" --update --file "$build/$target" -C "$build/$module" .
done
java -cp "$build/desktop:$build/game:$build/algorithm:$lib/*" RaceChecks
java -cp "$build/desktop:$build/game:$build/algorithm:$lib/*" RandomMapChecks
for target in algorithm.jar game.jar desktop-1.0.0.jar; do cp "$build/$target" "$lib/$target"; done
python3 - <<'PY'
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
root = Path('GridRift-demo (1)')
archive = root / 'GridRift-demo (1).zip'
temporary = archive.with_suffix('.zip.tmp')
with ZipFile(temporary, 'w', ZIP_DEFLATED) as z:
    for p in sorted(root.rglob('*')):
        relative = p.relative_to(root)
        if p.is_file() and p not in (archive, temporary) and relative.parts[0] != 'logs':
            z.write(p, relative.as_posix())
temporary.replace(archive)
PY
