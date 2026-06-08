package plugin.command;

import com.osroyale.Config;
import com.osroyale.content.skill.impl.magic.Spellbook;
import com.osroyale.game.plugin.extension.CommandExtension;
import com.osroyale.game.world.World;
import com.osroyale.game.world.entity.mob.Direction;
import com.osroyale.game.world.entity.mob.Mob;
import com.osroyale.game.world.entity.mob.data.LockType;
import com.osroyale.game.world.entity.mob.npc.Npc;
import com.osroyale.game.world.entity.mob.player.Player;
import com.osroyale.game.world.entity.mob.player.PlayerRight;
import com.osroyale.game.world.entity.mob.player.command.Command;
import com.osroyale.game.world.entity.mob.player.command.CommandParser;
import com.osroyale.game.world.items.Item;
import com.osroyale.game.world.items.ItemDefinition;
import com.osroyale.game.world.items.containers.ItemContainer;
import com.osroyale.game.world.position.Position;
import com.osroyale.net.packet.out.SendItemOnInterface;
import com.osroyale.net.packet.out.SendMessage;
import com.osroyale.net.packet.out.SendScrollbar;
import com.osroyale.net.packet.out.SendString;
import com.osroyale.util.MessageColor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class AdminCommandPlugin extends CommandExtension {

    private static final Logger logger = LoggerFactory.getLogger(AdminCommandPlugin.class);

    @Override
    public void register() {
        commands.add(new Command("pnpc") {
            @Override
            public void execute(Player player, CommandParser parser) {
                player.playerAssistant.transform(parser.nextInt());
            }
        });

        commands.add(new Command("spawnnpc") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    int id = parser.nextInt();
                    Npc npc = new Npc(id, player.getPosition(), Config.NPC_WALKING_RADIUS, Mob.DEFAULT_INSTANCE, Direction.NORTH);
                    npc.register();
                    npc.locking.lock(LockType.MASTER);

                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    File spawnFile = new File("data/def/npc-spawns-json", id + ".json");

                    try {
                        JsonArray spawns;
                        if (spawnFile.exists()) {
                            spawns = gson.fromJson(new FileReader(spawnFile), JsonArray.class);
                            if (spawns == null) {
                                spawns = new JsonArray();
                            }
                        } else {
                            spawns = new JsonArray();
                        }

                        JsonObject entry = new JsonObject();
                        entry.addProperty("id", id);
                        entry.addProperty("radius", String.valueOf(Config.NPC_WALKING_RADIUS));
                        entry.addProperty("facing", "NORTH");
                        entry.addProperty("convert-id", true);
                        entry.addProperty("instance", 0);

                        JsonObject pos = new JsonObject();
                        pos.addProperty("x", player.getPosition().getX());
                        pos.addProperty("y", player.getPosition().getY());
                        pos.addProperty("height", player.getPosition().getHeight());
                        entry.add("position", pos);

                        spawns.add(entry);

                        try (FileWriter writer = new FileWriter(spawnFile)) {
                            gson.toJson(spawns, writer);
                        }

                        logger.info("Saved NPC spawn {} at ({}, {}, {}) to {}.json",
                                id, player.getPosition().getX(), player.getPosition().getY(),
                                player.getPosition().getHeight(), id);
                        player.send(new SendMessage("NPC " + id + " spawned and saved to per-file."));
                    } catch (Exception e) {
                        logger.error("Failed to save NPC spawn to {}", spawnFile, e);
                        player.send(new SendMessage("Failed to save spawn: " + e.getMessage()));
                    }
                }
            }
        });

        commands.add(new Command("points") {
            @Override
            public void execute(Player player, CommandParser parser) {
                player.dragonfireCharges = 50;
                player.slayer.setPoints(50000);
                player.donation.setCredits(50000);
                player.votePoints = 500000;
                player.pestPoints = 500000;
                player.skillingPoints = 500000;
                player.message("Enjoy deh points");
            }
        });

        commands.add(new Command("demote") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    StringBuilder name = new StringBuilder(parser.nextString());
                    while (parser.hasNext()) {
                        name.append(" ").append(parser.nextString());
                    }
                    World.search(name.toString()).ifPresent(other -> {
                        if (PlayerRight.isDeveloper(other)) {
                            return;
                        }

                        other.right = PlayerRight.PLAYER;
                        other.dialogueFactory.sendStatement("You have been demoted!").execute();
                        player.message("demote was complete");
                    });
                } else {
                    player.message("Invalid command use; ::demote settings");
                }
            }
        });

        commands.add(new Command("save", "saveworld", "savegame") {
            @Override
            public void execute(Player player, CommandParser parser) {
                World.save();
                player.send(new SendMessage("All data has been successfully saved."));
            }
        });

        commands.add(new Command("bank") {
            @Override
            public void execute(Player player, CommandParser parser) {
                player.bank.open();
            }
        });

        commands.add(new Command("move") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext(3)) {
                    int x = parser.nextInt();
                    int y = parser.nextInt();
                    int z = parser.nextInt();
                    player.move(player.getPosition().transform(x, y, z));
                } else if (parser.hasNext(2)) {
                    int x = parser.nextInt();
                    int y = parser.nextInt();
                    int z = player.getHeight();
                    player.move(player.getPosition().transform(x, y, z));
                } else return;

                if (player.debug) {
                    player.send(new SendMessage("You have teleported to the coordinates: " + player.getPosition(), MessageColor.BLUE));
                }
            }
        });

        commands.add(new Command("tele") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext(3)) {
                    int x = parser.nextInt();
                    int y = parser.nextInt();
                    int z = parser.nextInt();
                    player.move(new Position(x, y, z));
                } else if (parser.hasNext(2)) {
                    int x = parser.nextInt();
                    int y = parser.nextInt();
                    int z = player.getHeight();
                    player.move(new Position(x, y, z));
                } else return;
                if (player.debug) {
                    player.send(new SendMessage("You have teleported to the coordinates: " + player.getPosition(), MessageColor.BLUE));
                }
            }
        });

        commands.add(new Command("spellbook") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    String spellbook = parser.nextString();
                    switch (spellbook.toUpperCase()) {
                        case "LUNAR":
                            player.spellbook = Spellbook.LUNAR;
                            break;
                        case "MODERN":
                            player.spellbook = Spellbook.MODERN;
                            break;
                        case "ANCIENT":
                            player.spellbook = Spellbook.ANCIENT;
                            break;
                    }
                    player.interfaceManager.setSidebar(Config.MAGIC_TAB, player.spellbook.getInterfaceId());
                }
            }
        });

        commands.add(new Command("starterbank") {
            @Override
            public void execute(Player player, CommandParser parser) {
                player.bank.clear();
                player.bank.addAll(Config.STARTER_BANK);
                System.arraycopy(Config.STARTER_BANK_AMOUNT, 0, player.bank.tabAmounts, 0, Config.STARTER_BANK_AMOUNT.length);
                player.bank.shift();
                player.bank.open();
            }
        });

        commands.add(new Command("bigbank") {
            @Override
            public void execute(Player player, CommandParser parser) {
                player.bank.clear();
                player.bank.addAll(Config.LEET_BANK_ITEMS);
                System.arraycopy(Config.LEET_BANK_AMOUNTS, 0, player.bank.tabAmounts, 0, Config.LEET_BANK_AMOUNTS.length);
                player.bank.shift();
                player.bank.open();
            }
        });

        commands.add(new Command("maxrng", "maxrange", "maxranged") {
            @Override
            public void execute(Player player, CommandParser parser) {
                addKit(player, "max ranged", MAX_RANGED_KIT);
            }
        });

        commands.add(new Command("maxmelee") {
            @Override
            public void execute(Player player, CommandParser parser) {
                addKit(player, "max melee", MAX_MELEE_KIT);
            }
        });

        commands.add(new Command("maxmagic", "maxmage") {
            @Override
            public void execute(Player player, CommandParser parser) {
                addKit(player, "max magic", MAX_MAGIC_KIT);
            }
        });

        commands.add(new Command("item", "pickup") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    int id = parser.nextInt();
                    int amount = 1;
                    if (parser.hasNext()) {
                        amount = Integer.parseInt(parser.nextString().toLowerCase().replace("k", "000").replace("m", "000000").replace("b", "000000000"));
                    }

                    final ItemDefinition def = ItemDefinition.get(id);

                    if (def == null || def.getName() == null) {
                        return;
                    }

                    if (def.getName().equalsIgnoreCase("null")) {
                        return;
                    }

                    player.inventory.add(id, amount);
                }
            }
        });

        commands.add(new Command("find", "give") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    final String name = parser.nextLine();
                    ItemContainer container = new ItemContainer(400, ItemContainer.StackPolicy.ALWAYS);
                    int count = 0;
                    for (final ItemDefinition def : ItemDefinition.DEFINITIONS) {
                        if (def == null || def.getName() == null || def.isNoted())
                            continue;
                        if (def.getName().toLowerCase().trim().contains(name)) {
                            container.add(new Item(def.getId()));
                            count++;
                            if (count == 400)
                                break;
                        }
                    }
                    player.send(new SendString("Search: <col=FF5500>" + name, 37506));
                    player.send(new SendString(String.format("Found <col=FF5500>%s</col> item%s", count, count != 1 ? "s" : ""), 37507));
                    player.send(new SendScrollbar(37520, count / 8 * 52 + ((count % 8) == 0 ? 0 : 52)));
                    player.send(new SendItemOnInterface(37521, container.getItems()));
                    player.interfaceManager.open(37500);
                    player.send(new SendMessage(String.format("Found %s item%s containing the key '%s'.", count, count != 1 ? "s" : "", name)));
                }
            }
        });

        commands.add(new Command("pos", "mypos", "coords") {
            @Override
            public void execute(Player player, CommandParser parser) {
                player.send(new SendMessage("Your location is: " + player.getPosition() + "."));
                System.out.println("Your location is: " + player.getPosition() + ".");
            }
        });
    }

    private static final Item[] MAX_RANGED_KIT = {
            new Item(27226), // Masori mask
            new Item(27229), // Masori body
            new Item(27232), // Masori chaps
            new Item(22109), // Ava's assembler
            new Item(19547), // Necklace of anguish
            new Item(26235), // Zaryte vambraces
            new Item(13237), // Pegasian boots
            new Item(11771), // Archer ring (i)
            new Item(20997), // Twisted bow
            new Item(26374), // Zaryte crossbow
            new Item(12926), // Toxic blowpipe
            new Item(11212, 500), // Dragon arrows
            new Item(9244, 500) // Dragon bolts (e)
    };

    private static final Item[] MAX_MELEE_KIT = {
            new Item(26382), // Torva full helm
            new Item(26384), // Torva platebody
            new Item(26386), // Torva platelegs
            new Item(21295), // Infernal cape
            new Item(19553), // Amulet of torture
            new Item(22981), // Ferocious gloves
            new Item(13239), // Primordial boots
            new Item(11773), // Berserker ring (i)
            new Item(22325), // Scythe of vitur
            new Item(22324), // Ghrazi rapier
            new Item(11802), // Armadyl godsword
            new Item(22322) // Avernic defender
    };

    private static final Item[] MAX_MAGIC_KIT = {
            new Item(21018), // Ancestral hat
            new Item(21021), // Ancestral robe top
            new Item(21024), // Ancestral robe bottom
            new Item(21295), // Infernal cape
            new Item(12002), // Occult necklace
            new Item(19544), // Tormented bracelet
            new Item(13235), // Eternal boots
            new Item(11770), // Seers ring (i)
            new Item(27275), // Tumeken's shadow
            new Item(21006), // Kodai wand
            new Item(6889), // Mages' book
            new Item(554, 1000), // Fire runes
            new Item(560, 1000), // Death runes
            new Item(565, 1000), // Blood runes
            new Item(566, 1000) // Soul runes
    };

    private static void addKit(Player player, String name, Item[] kit) {
        if (player.inventory.getFreeSlots() < kit.length) {
            player.send(new SendMessage("You need " + kit.length + " free inventory slots for the " + name + " kit."));
            return;
        }

        if (player.inventory.addAll(kit)) {
            player.send(new SendMessage("Added " + name + " kit to your inventory."));
        } else {
            player.send(new SendMessage("You need inventory space for the " + name + " kit."));
        }
    }

    @Override
    public boolean canAccess(Player player) {
        return PlayerRight.isAdministrator(player);
    }
}
