#!/usr/bin/env python3
"""Fix min(17)>max(5) for Blood rune (565) in npc_drops.json.
Uses exact line-based editing to avoid fuzzy-match damage.
"""
import json

with open('npc_drops.json', 'r') as f:
    data = json.load(f)

fixes = 0
for dt in data:
    for drop in dt.get('drops', []):
        if drop.get('item') == 565 and drop.get('minimum') == 17 and drop.get('maximum') == 5:
            # Swap: should be min=5, max=17
            drop['minimum'] = 5
            drop['maximum'] = 17
            fixes += 1

print(f"Fixed {fixes} entries")
if fixes > 0:
    with open('npc_drops.json', 'w') as f:
        json.dump(data, f, indent=2)
    print("File updated")
else:
    print("No fixes needed")
