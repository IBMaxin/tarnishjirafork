#!/usr/bin/env python3
"""Split monolithic npc_spawns.json into per-NPC-ID files.

Reads:  game-server/data/def/npc/npc_spawns.json  (34K lines)
Writes: game-server/data/def/npc-spawns-json/{npcId}.json

Each output file is an array (NPCs can have multiple spawn locations):
[
  {
    "id": 2042,
    "radius": "2",
    "facing": "SOUTH",
    "position": {"x": 3232, "y": 3109, "height": 0},
    "convert-id": true,
    "instance": 0
  }
]

Usage:
  python scripts/split_npc_spawns.py              # write files
  python scripts/split_npc_spawns.py --dry-run    # preview only, no writes
"""

import json
import sys
from collections import defaultdict
from pathlib import Path

INPUT = Path("game-server/data/def/npc/npc_spawns.json")
OUTPUT_DIR = Path("game-server/data/def/npc-spawns-json")


def main():
    dry_run = "--dry-run" in sys.argv

    if not INPUT.exists():
        print(f"ERROR: {INPUT} not found")
        sys.exit(1)

    with open(INPUT, "r", encoding="utf-8") as f:
        entries = json.load(f)

    print(f"Read {len(entries)} spawn entries from {INPUT}")

    # Group by NPC ID
    by_npc: dict[int, list[dict]] = defaultdict(list)

    for entry in entries:
        npc_id = entry.get("id")
        if npc_id is None or npc_id == 0:
            continue

        spawn = {
            "id": npc_id,
            "radius": str(entry.get("radius", "2")),
            "facing": entry.get("facing", "SOUTH"),
            "position": entry.get("position", {"x": 0, "y": 0, "height": 0}),
        }

        # Optional fields — only include when present
        if "convert-id" in entry:
            spawn["convert-id"] = entry["convert-id"]
        if "instance" in entry:
            spawn["instance"] = entry["instance"]

        by_npc[npc_id].append(spawn)

    # Write output
    if dry_run:
        print(f"\n[DRY RUN] Would write {len(by_npc)} files to {OUTPUT_DIR}/")
        sample_ids = sorted(by_npc.keys())[:5]
        for npc_id in sample_ids:
            spawns = by_npc[npc_id]
            locs = ", ".join(
                f"({s['position']['x']},{s['position']['y']})" for s in spawns
            )
            print(f"  {npc_id}.json — {len(spawns)} spawn(s): {locs}")
        if len(by_npc) > 5:
            print(f"  ... and {len(by_npc) - 5} more")
    else:
        OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

        written = 0
        for npc_id, spawns in sorted(by_npc.items()):
            out_path = OUTPUT_DIR / f"{npc_id}.json"
            with open(out_path, "w", encoding="utf-8") as f:
                json.dump(spawns, f, indent=2)
            written += 1

        print(f"Wrote {written} files to {OUTPUT_DIR}/")

    # Stats
    multi_spawn = sum(1 for v in by_npc.values() if len(v) > 1)
    with_instance = sum(
        1 for spawns in by_npc.values()
        if any("instance" in s for s in spawns)
    )
    with_convert_false = sum(
        1 for spawns in by_npc.values()
        if any(s.get("convert-id") is False for s in spawns)
    )

    print(f"  NPCs with multiple spawn locations: {multi_spawn}")
    print(f"  NPCs with instance field: {with_instance}")
    print(f"  NPCs with convert-id=false: {with_convert_false}")


if __name__ == "__main__":
    main()
