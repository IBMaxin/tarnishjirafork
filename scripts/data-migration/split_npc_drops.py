#!/usr/bin/env python3
"""Split monolithic npc_drops.json into per-NPC-ID files.

Reads:  game-server/data/def/npc/npc_drops.json  (107K lines)
Writes: game-server/data/def/npc-drops-json/{npcId}.json

Each output file:
{
  "npc_id": 2042,
  "rare_table": true,
  "roll-data": [100, 200, ...],   // optional
  "drops": [
    {"item": 22124, "minimum": 2, "maximum": 2, "type": "ALWAYS", "chance": 128.0}
  ]
}

Usage:
  python scripts/split_npc_drops.py              # write files
  python scripts/split_npc_drops.py --dry-run    # preview only, no writes
"""

import json
import os
import sys
from collections import defaultdict
from pathlib import Path

INPUT = Path("game-server/data/def/npc/npc_drops.json")
OUTPUT_DIR = Path("game-server/data/def/npc-drops-json")


def main():
    dry_run = "--dry-run" in sys.argv

    if not INPUT.exists():
        print(f"ERROR: {INPUT} not found")
        sys.exit(1)

    with open(INPUT, "r", encoding="utf-8") as f:
        entries = json.load(f)

    print(f"Read {len(entries)} drop table entries from {INPUT}")

    # Group by NPC ID. One entry can have multiple NPC IDs sharing the same table.
    # We write one file per NPC ID, each containing that NPC's drop table.
    by_npc: dict[int, dict] = {}

    for entry in entries:
        npc_ids = entry.get("id", [])
        if isinstance(npc_ids, int):
            npc_ids = [npc_ids]

        rare_table = entry.get("rare_table", False)
        drops = entry.get("drops", [])
        roll_data = entry.get("roll-data", None)

        for npc_id in npc_ids:
            file_data = {
                "npc_id": npc_id,
                "rare_table": rare_table,
                "drops": drops,
            }
            if roll_data is not None:
                file_data["roll-data"] = roll_data

            by_npc[npc_id] = file_data

    # Write output
    if dry_run:
        print(f"\n[DRY RUN] Would write {len(by_npc)} files to {OUTPUT_DIR}/")
        sample_ids = sorted(by_npc.keys())[:5]
        for npc_id in sample_ids:
            d = by_npc[npc_id]
            print(f"  {npc_id}.json — {len(d['drops'])} drops, rare_table={d['rare_table']}"
                  + (", has roll-data" if "roll-data" in d else ""))
        if len(by_npc) > 5:
            print(f"  ... and {len(by_npc) - 5} more")
    else:
        OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

        written = 0
        for npc_id, file_data in sorted(by_npc.items()):
            out_path = OUTPUT_DIR / f"{npc_id}.json"
            with open(out_path, "w", encoding="utf-8") as f:
                json.dump(file_data, f, indent=2)
            written += 1

        print(f"Wrote {written} files to {OUTPUT_DIR}/")

    # Stats
    total_drops = sum(len(v["drops"]) for v in by_npc.values())
    multi_id_entries = sum(
        1 for e in entries
        if isinstance(e.get("id"), list) and len(e["id"]) > 1
    )
    roll_data_entries = sum(1 for e in entries if "roll-data" in e)
    print(f"  Total drop entries across all files: {total_drops}")
    print(f"  Multi-ID entries (shared tables): {multi_id_entries}")
    print(f"  Entries with roll-data: {roll_data_entries}")


if __name__ == "__main__":
    main()
