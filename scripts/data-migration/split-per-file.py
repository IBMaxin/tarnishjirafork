#!/usr/bin/env python3
"""
split-per-file.py
=================
Generates a reconciliation report and optionally creates per-file entries
for items/NPCs that exist in the old monolithic system but are missing from
the new per-file system.

Usage:
    # Dry-run: report only
    python split-per-file.py

    # After reviewing the report, apply the changes:
    python split-per-file.py --apply

What it does:
  1. Loads IDs from old monolithic files and per-file directories
  2. Writes `reconciliation-report.json` with old-only and new-only lists
  3. If --apply is given, writes missing per-file entries for:
       - data/def/items-json/ (from item_definitions.json)
       - data/def/monsters-json/ (from npc_definitions.json)
"""
import argparse
import json
import os
import sys
from pathlib import Path

# ── Config ───────────────────────────────────────────────────────────────
REPO = Path(__file__).parent

ITEM_MONO  = REPO / "game-server/data/def/item/item_definitions.json"
ITEM_DIR   = REPO / "game-server/data/def/items-json/"

NPC_MONO   = REPO / "game-server/data/def/npc/npc_definitions.json"
NPC_DIR    = REPO / "game-server/data/def/monsters-json/"

REPORT     = REPO / "reconciliation-report.json"


# ── Helpers ──────────────────────────────────────────────────────────────

def load_mono_ids(path: Path):
    with open(path, encoding="utf-8") as f:
        arr = json.load(f)
    return {e["id"]: e for e in arr if "id" in e}


def load_perfile_ids(dir_: Path):
    ids = {}
    if not dir_.exists():
        return ids
    for p in dir_.iterdir():
        if p.suffix == ".json":
            try:
                with open(p, encoding="utf-8") as f:
                    data = json.load(f)
                ids[data["id"]] = data
            except Exception:
                pass
    return ids


def diff(old: dict, new: dict):
    old_ids = {int(k) for k in old.keys()}
    new_ids = {int(k) for k in new.keys()}
    old_only = old_ids - new_ids
    new_only = new_ids - old_ids
    return sorted(old_only), sorted(new_only)


# ── Field mappers ────────────────────────────────────────────────────────
"""
The old monolithic and new per-file systems have slightly different shapes.
These functions normalise an old entry into the new per-file format so that
existing loaders can understand it.
"""

def map_item(old: dict) -> dict:
    """Map old monolithic item fields to new per-file format."""
    new = {
        "id": old["id"],
        "name": old.get("name", f"Unknown {old['id']}"),
        "members": old.get("members", True),
        "tradeable": old.get("tradeable", True),
        "tradeable_on_ge": old.get("tradeable-on-ge",
                                   old.get("tradeable_on_ge", True)),
        "stackable": old.get("stackable", False),
        "noted": old.get("noted", False),
        "noteable": old.get("noteable", False),
        "placeholder": old.get("placeholder", False),
        "equipable": old.get("equipable", False),
        "equipable_by_player": old.get("equipable-by-player",
                                       old.get("equipable_by_player", False)),
        "equipable_weapon": old.get("equipable-weapon",
                                    old.get("equipable_weapon", False)),
        "cost": old.get("base-value", old.get("cost", 0)),
        "lowalch": old.get("low-alch", old.get("lowalch", 0)),
        "highalch": old.get("high-alch", old.get("highalch", 0)),
        "weight": old.get("weight", 0.0),
        "buy_limit": old.get("buy-limit", old.get("buy_limit", 0)),
        "quest_item": old.get("quest-item",
                              old.get("quest-item", False)),
        "release_date": old.get("release-date",
                                old.get("release_date", "2003-02-04")),
        "duplicate": old.get("duplicate", False),
        "examine": old.get("examine", ""),
        "icon": old.get("icon", ""),
        "wiki_name": old.get("wiki_name", old.get("name", "")),
        "wiki_url": old.get("wiki_url", ""),
        "equipment": old.get("equipment", None),
        "weapon": old.get("weapon", None),
    }
    # Drop old-style nested keys the new loader doesn't need
    new.pop("destroy-message", None)
    new.pop("destroyable", None)
    new.pop("linked_id_item", None)
    new.pop("linked_id_noted", None)
    new.pop("linked_id_placeholder", None)
    new.pop("street-value", None)
    new.pop("barrow-item", None)
    return new


def map_npc(old: dict) -> dict:
    """
    Map old monolithic NPC fields to new per-file format.
    The new loader typically expects the same keys, so we just clean up
    any old-style hyphenated fields.
    """
    new = dict(old)
    # Harmonise common field name variants
    replace = {}
    for key in list(new.keys()):
        if "-" in key:
            snake = key.replace("-", "_")
            replace[snake] = new.pop(key)
    new.update(replace)
    return new


# ── Main ─────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--apply",
        action="store_true",
        help="Actually write per-file JSON entries for old-only IDs")
    args = parser.parse_args()

    # --- Items ---
    if not ITEM_MONO.exists():
        print(f"Missing: {ITEM_MONO}")
        sys.exit(1)
    item_mono = load_mono_ids(ITEM_MONO)
    item_new  = load_perfile_ids(ITEM_DIR)
    item_old_only, item_new_only = diff(item_mono, item_new)

    applied_items = 0
    if args.apply and item_old_only:
        ITEM_DIR.mkdir(parents=True, exist_ok=True)
        for iid in item_old_only:
            out = ITEM_DIR / f"{iid}.json"
            with open(out, "w", encoding="utf-8") as f:
                json.dump(map_item(item_mono[iid]), f, indent=4, ensure_ascii=False)
            applied = True

    # --- NPCs ---
    if not NPC_MONO.exists():
        print(f"Missing: {NPC_MONO}")
        sys.exit(1)
    npc_mono = load_mono_ids(NPC_MONO)
    npc_new  = load_perfile_ids(NPC_DIR)
    npc_old_only, npc_new_only = diff(npc_mono, npc_new)

    applied_npcs = 0
    if args.apply and npc_old_only:
        NPC_DIR.mkdir(parents=True, exist_ok=True)
        for nid in npc_old_only:
            out = NPC_DIR / f"{nid}.json"
            with open(out, "w", encoding="utf-8") as f:
                json.dump(map_npc(npc_mono[nid]), f, indent=4, ensure_ascii=False)
            applied_npcs += 1

    # --- Report ---
    report = {
        "items": {
            "monolithic_count": len(item_mono),
            "perfile_count":    len(item_new),
            "old_only_count":   len(item_old_only),
            "new_only_count":   len(item_new_only),
            "old_only":         item_old_only,
            "new_only":         item_new_only,
        },
        "npcs": {
            "monolithic_count": len(npc_mono),
            "perfile_count":    len(npc_new),
            "old_only_count":   len(npc_old_only),
            "new_only_count":   len(npc_new_only),
            "old_only":         npc_old_only,
            "new_only":         npc_new_only,
        },
    }
    with open(REPORT, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2, ensure_ascii=False)

    # --- Summary ---
    print("── Reconciliation summary ──────────────────────────────────────")
    print()
    print("Items:")
    print(f"  Monolithic file : {len(item_mono)}")
    print(f"  Per-file dir    : {len(item_new)}")
    print(f"  Old-only (gap)  : {len(item_old_only)}")
    print(f"  New-only (drift): {len(item_new_only)}")
    if item_old_only:
        print(f"\n  Old-only IDs (sample): {item_old_only[:20]}")
    print()
    print("NPCs:")
    print(f"  Monolithic file : {len(npc_mono)}")
    print(f"  Per-file dir    : {len(npc_new)}")
    print(f"  Old-only (gap)  : {len(npc_old_only)}")
    print(f"  New-only (drift): {len(npc_new_only)}")
    if npc_old_only:
        print(f"\n  Old-only IDs (sample): {npc_old_only[:20]}")
    print()
    print(f"Full report: {REPORT}")
    print()
    if args.apply:
        print(f"Wrote {applied_items} item files to {ITEM_DIR}")
        print(f"Wrote {applied_npcs} NPC files to {NPC_DIR}")
    else:
        print("Dry-run only. Add --apply to write the per-file entries.")
    print()


if __name__ == "__main__":
    main()
