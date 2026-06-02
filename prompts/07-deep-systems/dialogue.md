# Dialogue System Audit

**Goal:** Map the DialogueFactory system — all dialogue types, chain integrity, and injection risks.

**Docs:** `AGENTS.md`, `code_index.json`

---

## Target Files

| File | Relevance |
|------|-----------|
| `game-server/src/main/java/com/osroyale/content/dialogue/DialogueFactory.java` | Core factory — builds all dialogue chains |
| `game-server/src/main/java/com/osroyale/content/dialogue/Dialogue.java` | Base dialogue class |
| `game-server/src/main/java/com/osroyale/content/dialogue/NpcDialogue.java` | NPC chat dialogue |
| `game-server/src/main/java/com/osroyale/content/dialogue/PlayerDialogue.java` | Player chat dialogue |
| `game-server/src/main/java/com/osroyale/content/dialogue/StatementDialogue.java` | Statement/notification dialogue |
| `game-server/src/main/java/com/osroyale/content/dialogue/OptionDialogue.java` | Multiple-choice dialogue |
| `game-server/src/main/java/com/osroyale/content/dialogue/ItemDialogue.java` | Item display dialogue |
| `game-server/src/main/java/com/osroyale/content/dialogue/ChatBoxItemDialogue.java` | Chatbox item dialogue |
| `game-server/src/main/java/com/osroyale/content/dialogue/InformationDialogue.java` | Information display dialogue |
| `game-server/src/main/java/com/osroyale/content/dialogue/impl/*.java` | All specific dialogue implementations (RoyalKing, Vote, Prestige, Construction, Kamfreena, Lootshare, Nieve, Clanmaster, ClanRank, WellOfGoodwill) |
| `game-server/src/main/java/com/osroyale/net/packet/in/DialoguePacketListener.java` | Incoming dialogue continuation packet |
| `game-server/src/main/java/com/osroyale/net/packet/out/SendPlayerDialogueHead.java` | Outgoing dialogue head packet |

---

## Architecture

```
DialogueFactory
├── sendNpcChat(npcId, expression, lines...) → NpcDialogue
├── sendPlayerChat(expression, lines...) → PlayerDialogue  
├── sendStatement(lines...) → StatementDialogue
├── sendOption(lines..., Runnable...) → OptionDialogue
├── sendItemStatement(title, item) → ItemDialogue
├── sendInformationBox(title, message) → InformationDialogue
├── onAction(Runnable) → sets callback for next advance
├── execute() → finalizes and sends the dialogue chain
└── clear() → clears the chain (cancel)
```

All dialogue is server-driven. The client sends "continue" packets to advance. The server builds the next dialogue frame.

---

## Steps

### 1. Map all dialogue implementations

```bash
cd /mnt/c/Users/bob/IdeaProjects/tarnishjirafork

# List all dialogue impls
ls game-server/src/main/java/com/osroyale/content/dialogue/impl/

# Find all references to DialogueFactory in plugins
grep -rn "dialogueFactory\|DialogueFactory" \
  game-server/plugins/ --include="*.java" | wc -l
```

### 2. Audit DialoguePacketListener for validation

```bash
cat game-server/src/main/java/com/osroyale/net/packet/in/DialoguePacketListener.java
```

This is the packet handler for dialogue continuation. Check:
- Does it validate the player is actually in a dialogue?
- Can spoofed continue packets trigger actions out of sequence?

### 3. Check dialogue chain integrity

```bash
# Can dialogue chains be interrupted mid-execution?
grep -rn "dialogueFactory\|dialogueFactory\." \
  game-server/src/main/java/com/osroyale/ --include="*.java" \
  | grep -v "import\|DialogueFactory.java\|\.class"
```

### 4. Check for format injection in dialogue text

```bash
# Are player names or user input inserted into dialogue without sanitization?
grep -rn "formatName\|getName()\|getUsername()" \
  game-server/src/main/java/com/osroyale/content/dialogue/ --include="*.java" \
  | grep -v "import"
```

RSPS dialogue supports format tags like `<col=FF0000>`, `<shad=0>`, `<img=1>`. If player names are inserted unsanitized into dialogue, a player named `<img=1>hacker` could inject images or formatting.

### 5. Check Runnable callback safety

```bash
# OptionDialogue stores Runnables for each option
# Check if option callbacks can be triggered out of order
cat game-server/src/main/java/com/osroyale/content/dialogue/OptionDialogue.java
```

### 6. Audit specific dialogue types for logic errors

```bash
# RoyalKingDialogue — what does it do?
cat game-server/src/main/java/com/osroyale/content/dialogue/impl/RoyalKingDialogue.java

# PrestigeDialogue — can it be exploited?
cat game-server/src/main/java/com/osroyale/content/dialogue/impl/PrestigeDialogue.java

# VoteDialogue — triggers VoteService, any race conditions?
cat game-server/src/main/java/com/osroyale/content/dialogue/impl/VoteDialogue.java
```

---

## Findings

### F1: DialoguePacketListener — check for out-of-order continue [MEDIUM]

If `DialoguePacketListener` doesn't validate that a dialogue is active, a spoofed continue packet could trigger stale callbacks. Read the handler to verify.

### F2: Player name format injection risk [MEDIUM]

Multiple dialogues insert `player.getName()` or `player.getUsername()` into dialogue text. If names can contain format tags (e.g., a player named `<col=FF0000>Red`), these tags render in the client. Check if username validation (`[a-zA-Z0-9 ]`) prevents this.

### F3: VoteDialogue → VoteService DB query [LOW]

VoteDialogue triggers `VoteService.claimReward()` which makes a live MySQL query. If the dialogue is spammed, it hits the rate limiter (`databaseRequest.elapsed(1, TimeUnit.MINUTES)`) but each attempt opens a DB connection.

---

## Client Impact

Server-only. All dialogue frames are constructed server-side and sent as packets. The client only sends "continue" (spacebar/click) packets.

---

## Verify

- [ ] DialoguePacketListener validates dialogue is active before processing continue
- [ ] Option callbacks cannot be triggered out of sequence
- [ ] Player names in dialogue are validated against format tag injection
- [ ] Dialogue chains clean up properly on logout/teleport
- [ ] VoteDialogue rate limiter effective against spam
- [ ] No dialogue type exposes privileged actions to unauthorized players
