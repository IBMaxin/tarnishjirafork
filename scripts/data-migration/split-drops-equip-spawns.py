#!/usr/bin/env python3
"""
split-per-file-drops-equip-spawns.py
=====================================
Creates per-file JSON entries for drops, equipment, and NPC spawns.

Usage:
    # Dry-run: report only
    python split-per-file-drops-equip-spawns.py

    # Apply the splits:
    python split-per-file-drops-equip-spawns.py --apply

What it does:
  1. Reads monolithic npc_drops.json, equipment_definitions.json, npc_spawns.json
  2. Reports counts and any anomalies
  3. --apply writes one file per entity ID into new per-file directories:
       - data/def/npc-drops-json/    (one file per NPC ID)
       - data/def/equipment-json/    (one file per item ID)
       - data/def/npc-spawns-json/   (one file per NPC ID, array of spawn locations)
"""
import argparse
import json
import sys
from collections import defaultdict
from pathlib import Path

# ── Config ───────────────────────────────────────────────────────────────
REPO = Path(__file__).parent

DROPS_MONO = REPO / "game-server/data/def/npc/npc_drops.json"
DROPS_DIR  = REPO / "game-server/data/def/npc-drops-json/"

EQUIP_MONO = REPO / "game-server/data/def/equipment/equipment_definitions.json"
EQUIP_DIR  = REPO / "game-server/data/def/equipment-json/"

SPAWNS_MONO = REPO / "game-server/data/def/npc/npc_spawns.json"
SPAWNS_DIR  = REPO / "game-server/data/def/npc-spawns-json/"


# ── NPC Drops ────────────────────────────────────────────────────────────
def split_drops(apply: bool) -> dict:
    """Split npc_drops.json into one file per NPC ID.

    The monolithic file has entries like:
        { "id": [8060], "rare_table": true, "drops": [...] }
    where "id" is an ARRAY of NPC IDs that share the same drop table.

    We expand multi-ID entries so each NPC gets its own file containing the
    full drop table. Drop items use both "item" and "id" fields — we normalise
    to "item" in the output.

    Returns a report dict.
    """
    with open(DROPS_MONO, encoding="utf-8") as f:
        data = json.load(f)

    npc_drops: dict[int, dict] = {}  # npc_id -> merged drop entry
    multi_id_count = 0
    total_drops = 0
    id_field_count = 0  # drops using "id" instead of "item"

    for entry in data:
        npc_ids = entry.get("id", [])
        if not isinstance(npc_ids, list):
            npc_ids = [npc_ids]

        # Normalise drops: "id" → "item"
        normalised_drops = []
        for drop in entry.get("drops", []):
            d = dict(drop)
            if "id" in d and "item" not in d:
                d["item"] = d.pop("id")
                id_field_count += 1
            elif "id" in d and "item" in d:
                d.pop("id")  # "item" takes precedence
                id_field_count += 1
            normalised_drops.append(d)
            total_drops += 1

        out_entry = {
            "npc_ids": sorted(npc_ids),
            "rare_table": entry.get("rare_table", False),
            "drops": normalised_drops,
        }

        if len(npc_ids) > 1:
            multi_id_count += 1

        for nid in npc_ids:
            if nid in npc_drops:
                # Merge drops (shouldn't happen but handle gracefully)
                existing = npc_drops[nid]["drops"]
                existing_ids = {d.get("item") for d in existing}
                for d in normalised_drops:
                    if d.get("item") not in existing_ids:
                        existing.append(d)
                npc_drops[nid]["rare_table"] = npc_drops[nid]["rare_table"] or entry.get("rare_table", False)
            else:
                npc_drops[nid] = {
                    "npc_id": nid,
                    "rare_table": entry.get("rare_table", False),
                    "drops": normalised_drops,
                }

    # Write files
    files_written = 0
    if apply and npc_drops:
        DROPS_DIR.mkdir(parents=True, exist_ok=True)
        for nid in sorted(npc_drops.keys()):
            out = DROPS_DIR / f"{nid}.json"
            with open(out, "w", encoding="utf-8") as f:
                json.dump(npc_drops[nid], f, indent=4, ensure_ascii=False)
            files_written += 1

    return {
        "monolithic_entries": len(data),
        "unique_npc_ids": len(npc_drops),
        "multi_id_entries": multi_id_count,
        "total_drops": total_drops,
        "drops_normalised_from_id": id_field_count,
        "files_written": files_written,
    }


# ── Equipment ────────────────────────────────────────────────────────────
def split_equipment(apply: bool) -> dict:
    """Split equipment_definitions.json into one file per item ID.

    Simple: each entry has a single integer "id".
    Returns a report dict.
    """
    with open(EQUIP_MONO, encoding="utf-8") as f:
        data = json.load(f)

    equip_by_id: dict[int, dict] = {}
    missing_ids = []

    for entry in data:
        eid = entry.get("id")
        if eid is None:
            missing_ids.append(entry)
            continue
        equip_by_id[eid] = entry

    # Write files
    files_written = 0
    if apply and equip_by_id:
        EQUIP_DIR.mkdir(parents=True, exist_ok=True)
        for eid in sorted(equip_by_id.keys()):
            out = EQUIP_DIR / f"{eid}.json"
            with open(out, "w", encoding="utf-8") as f:
                json.dump(equip_by_id[eid], f, indent=4, ensure_ascii=False)
            files_written += 1

    return {
        "monolithic_entries": len(data),
        "unique_item_ids": len(equip_by_id),
        "missing_ids": len(missing_ids),
        "files_written": files_written,
    }


# ── NPC Spawns ───────────────────────────────────────────────────────────
def split_spawns(apply: bool) -> dict:
    """Split npc_spawns.json into one file per NPC ID.

    An NPC ID can appear multiple times (multiple spawn locations).
    We group by ID and write an array of spawn objects per file.

    Returns a report dict.
    """
    with open(SPAWNS_MONO, encoding="utf-8") as f:
        data = json.load(f)

    spawns_by_npc: dict[int, list[dict]] = defaultdict(list)
    total_spawns = 0
    missing_ids = []

    for entry in data:
        nid = entry.get("id")
        if nid is None:
            missing_ids.append(entry)
            continue
        spawns_by_npc[nid].append(entry)
        total_spawns += 1

    multi_spawn_npcs = sum(1 for v in spawns_by_npc.values() if len(v) > 1)

    # Write files
    files_written = 0
    if apply and spawns_by_npc:
        SPAWNS_DIR.mkdir(parents=True, exist_ok=True)
        for nid in sorted(spawns_by_npc.keys()):
            out = SPAWNS_DIR / f"{nid}.json"
            with open(out, "w", encoding="utf-8") as f:
                json.dump(spawns_by_npc[nid], f, indent=4, ensure_ascii=False)
            files_written += 1

    return {
        "monolithic_entries": len(data),
        "unique_npc_ids": len(spawns_by_npc),
        "multi_spawn_npcs": multi_spawn_npcs,
        "total_spawns": total_spawns,
        "missing_ids": len(missing_ids),
        "files_written": files_written,
    }


# ── Main ─────────────────────────────────────────────────────────────────
def main():
    parser = argparse.ArgumentParser(
        description="Split drops, equipment, and spawns into per-file JSON"
    )
    parser.add_argument(
        "--apply", action="store_true",
        help="Actually write the per-file JSON entries"
    )
    args = parser.parse_args()

    # Validate monolithic files exist
    for path, label in [
        (DROPS_MONO, "npc_drops.json"),
        (EQUIP_MONO, "equipment_definitions.json"),
        (SPAWNS_MONO, "npc_spawns.json"),
    ]:
        if not path.exists():
            print(f"Missing: {path}")
            sys.exit(1)

    # Run splits
    drops_report = split_drops(args.apply)
    equip_report = split_equipment(args.apply)
    spawns_report = split_spawns(args.apply)

    # Summary
    print()
    print("═" * 60)
    print("  Per-File Split Report")
    print("═" * 60)

    print()
    print("NPC Drops:")
    print(f"  Monolithic entries   : {drops_report['monolithic_entries']}")
    print(f"  Unique NPC IDs       : {drops_report['unique_npc_ids']}")
    print(f"  Multi-ID entries     : {drops_report['multi_id_entries']} (shared drop tables)")
    print(f"  Total drop items     : {drops_report['total_drops']}")
    print(f"  Normalised 'id'→'item': {drops_report['drops_normalised_from_id']}")
    if args.apply:
        print(f"  Files written        : {drops_report['files_written']}")
    print(f"  Output dir           : {DROPS_DIR}")

    print()
    print("Equipment:")
    print(f"  Monolithic entries   : {equip_report['monolithic_entries']}")
    print(f"  Unique item IDs      : {equip_report['unique_item_ids']}")
    print(f"  Missing ID entries   : {equip_report['missing_ids']}")
    if args.apply:
        print(f"  Files written        : {equip_report['files_written']}")
    print(f"  Output dir           : {EQUIP_DIR}")

    print()
    print("NPC Spawns:")
    print(f"  Monolithic entries   : {spawns_report['monolithic_entries']}")
    print(f"  Unique NPC IDs       : {spawns_report['unique_npc_ids']}")
    print(f"  Multi-spawn NPCs     : {spawns_report['multi_spawn_npcs']} (multiple locations)")
    print(f"  Missing ID entries   : {spawns_report['missing_ids']}")
    if args.apply:
        print(f"  Files written        : {spawns_report['files_written']}")
    print(f"  Output dir           : {SPAWNS_DIR}")

    print()
    if not args.apply:
        print("Dry-run only. Add --apply to write the per-file entries.")
    else:
        total = (drops_report["files_written"] +
                 equip_report["files_written"] +
                 spawns_report["files_written"])
        print(f"Total files written: {total}")
    print()


if __name__ == "__main__":
    main()
