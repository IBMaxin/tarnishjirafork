#!/usr/bin/env python3
"""Remove spawn entries that reference NPC IDs not in npc_definitions.json.
Saves a backup as npc_spawns.json.bak first."""
import json, shutil, os

defs_file = 'npc_definitions.json'
spawns_file = 'npc_spawns.json'
backup_file = spawns_file + '.bak'

# Load known NPC IDs
with open(defs_file) as f:
    defs = json.load(f)
known_ids = {d['id'] for d in defs}
print(f"Known NPC IDs: {len(known_ids)}")

# Load spawns
with open(spawns_file) as f:
    spawns = json.load(f)
original_count = len(spawns)
print(f"Total spawn entries: {original_count}")

# Filter
filtered = [s for s in spawns if s.get('id') in known_ids]
removed = original_count - len(filtered)
print(f"Removed {removed} orphan spawns, kept {len(filtered)}")

if removed > 0:
    shutil.copy2(spawns_file, backup_file)
    print(f"Backup saved as {backup_file}")
    with open(spawns_file, 'w') as f:
        json.dump(filtered, f, indent=2)
    print("Filtered file written")
else:
    print("No changes needed")
