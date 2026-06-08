package com.osroyale.game.plugin;

import com.osroyale.game.event.bus.PlayerDataBus;
import com.osroyale.game.event.impl.*;
import com.osroyale.game.world.entity.mob.npc.Npc;
import com.osroyale.game.world.entity.mob.player.Player;
import com.osroyale.game.world.entity.mob.player.command.CommandParser;
import com.osroyale.game.world.items.Item;
import com.osroyale.game.world.items.ground.GroundItem;
import com.osroyale.game.world.object.GameObject;
import com.osroyale.game.world.position.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PluginContextDispatchTest {

    private PlayerDataBus dataBus;
    private Player player;

    @BeforeEach
    void setUp() throws Exception {
        drainStaticSet(PlayerDataBus.class, "chain");
        dataBus = PlayerDataBus.getInstance();
        player = mock(Player.class);
        when(player.getName()).thenReturn("test");
    }

    @Test
    void buttonClickEvent_dispatchesToOnClick() {
        AtomicInteger captured = new AtomicInteger(-1);
        PluginContext plugin = new PluginContext() {
            @Override
            protected boolean onClick(Player player, int button) {
                captured.set(button);
                return true;
            }
        };
        dataBus.subscribe(plugin);
        boolean handled = dataBus.publish(player, new ButtonClickEvent(42));
        assertTrue(handled);
        assertEquals(42, captured.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void itemClickEvent_dispatchesToCorrectHandler(int type) {
        AtomicInteger capturedType = new AtomicInteger(-1);
        PluginContext plugin = new PluginContext() {
            @Override
            protected boolean firstClickItem(Player player, ItemClickEvent event) {
                capturedType.set(1);
                return true;
            }
            @Override
            protected boolean secondClickItem(Player player, ItemClickEvent event) {
                capturedType.set(2);
                return true;
            }
            @Override
            protected boolean thirdClickItem(Player player, ItemClickEvent event) {
                capturedType.set(3);
                return true;
            }
            @Override
            protected boolean fourthClickItem(Player player, ItemClickEvent event) {
                capturedType.set(4);
                return true;
            }
        };
        dataBus.subscribe(plugin);
        boolean handled = dataBus.publish(player, new ItemClickEvent(type, new Item(995, 1), 0));
        assertTrue(handled);
        assertEquals(type, capturedType.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void npcClickEvent_dispatchesToCorrectHandler(int type) {
        AtomicInteger capturedType = new AtomicInteger(-1);
        Npc npc = mock(Npc.class);
        PluginContext plugin = new PluginContext() {
            @Override
            protected boolean firstClickNpc(Player player, NpcClickEvent event) {
                capturedType.set(1);
                return true;
            }
            @Override
            protected boolean secondClickNpc(Player player, NpcClickEvent event) {
                capturedType.set(2);
                return true;
            }
            @Override
            protected boolean thirdClickNpc(Player player, NpcClickEvent event) {
                capturedType.set(3);
                return true;
            }
            @Override
            protected boolean fourthClickNpc(Player player, NpcClickEvent event) {
                capturedType.set(4);
                return true;
            }
        };
        dataBus.subscribe(plugin);
        boolean handled = dataBus.publish(player, new NpcClickEvent(type, npc));
        assertTrue(handled);
        assertEquals(type, capturedType.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void objectClickEvent_dispatchesToCorrectHandler(int type) {
        AtomicInteger capturedType = new AtomicInteger(-1);
        GameObject object = mock(GameObject.class);
        PluginContext plugin = new PluginContext() {
            @Override
            protected boolean firstClickObject(Player player, ObjectClickEvent event) {
                capturedType.set(1);
                return true;
            }
            @Override
            protected boolean secondClickObject(Player player, ObjectClickEvent event) {
                capturedType.set(2);
                return true;
            }
            @Override
            protected boolean thirdClickObject(Player player, ObjectClickEvent event) {
                capturedType.set(3);
                return true;
            }
        };
        dataBus.subscribe(plugin);
        boolean handled = dataBus.publish(player, new ObjectClickEvent(type, object));
        assertTrue(handled);
        assertEquals(type, capturedType.get());
    }

    @Test
    void itemOnItemEvent_dispatchesToItemOnItem() {
        AtomicReference<ItemOnItemEvent> captured = new AtomicReference<>();
        PluginContext plugin = new PluginContext() {
            @Override
            protected boolean itemOnItem(Player player, ItemOnItemEvent event) {
                captured.set(event);
                return true;
            }
        };
        dataBus.subscribe(plugin);
        Item used = new Item(1, 1);
        Item with = new Item(2, 1);
        boolean handled = dataBus.publish(player, new ItemOnItemEvent(used, 0, with, 1));
        assertTrue(handled);
        assertSame(used, captured.get().getUsed());
        assertSame(with, captured.get().getWith());
    }

    @Test
    void itemOnNpcEvent_dispatchesToItemOnNpc() {
        AtomicReference<ItemOnNpcEvent> captured = new AtomicReference<>();
        Npc npc = mock(Npc.class);
        PluginContext plugin = new PluginContext() {
            @Override
            protected boolean itemOnNpc(Player player, ItemOnNpcEvent event) {
                captured.set(event);
                return true;
            }
        };
        dataBus.subscribe(plugin);
        Item used = new Item(1, 1);
        boolean handled = dataBus.publish(player, new ItemOnNpcEvent(npc, used, 0));
        assertTrue(handled);
        assertSame(npc, captured.get().getNpc());
        assertSame(used, captured.get().getUsed());
    }

    @Test
    void itemOnObjectEvent_dispatchesToItemOnObject() {
        AtomicReference<ItemOnObjectEvent> captured = new AtomicReference<>();
        GameObject object = mock(GameObject.class);
        PluginContext plugin = new PluginContext() {
            @Override
            protected boolean itemOnObject(Player player, ItemOnObjectEvent event) {
                captured.set(event);
                return true;
            }
        };
        dataBus.subscribe(plugin);
        Item used = new Item(1, 1);
        boolean handled = dataBus.publish(player, new ItemOnObjectEvent(used, 0, object));
        assertTrue(handled);
        assertSame(object, captured.get().getObject());
        assertSame(used, captured.get().getUsed());
    }

    @Test
    void itemOnPlayerEvent_dispatchesToItemOnPlayer() {
        AtomicReference<ItemOnPlayerEvent> captured = new AtomicReference<>();
        Player other = mock(Player.class);
        PluginContext plugin = new PluginContext() {
            @Override
            protected boolean itemOnPlayer(Player player, ItemOnPlayerEvent event) {
                captured.set(event);
                return true;
            }
        };
        dataBus.subscribe(plugin);
        Item used = new Item(1, 1);
        boolean handled = dataBus.publish(player, new ItemOnPlayerEvent(other, used, 0));
        assertTrue(handled);
        assertSame(other, captured.get().getOther());
        assertSame(used, captured.get().getUsed());
    }

    @Test
    void commandEvent_dispatchesToHandleCommand() {
        AtomicReference<CommandParser> captured = new AtomicReference<>();
        PluginContext plugin = new PluginContext() {
            @Override
            protected boolean handleCommand(Player player, CommandParser parser) {
                captured.set(parser);
                return true;
            }
        };
        dataBus.subscribe(plugin);
        CommandParser parser = mock(CommandParser.class);
        boolean handled = dataBus.publish(player, new CommandEvent(parser));
        assertTrue(handled);
        assertSame(parser, captured.get());
    }

    @ParameterizedTest
    @CsvSource({
        "1, firstClickItemContainer",
        "2, secondClickItemContainer",
        "3, thirdClickItemContainer",
        "4, fourthClickItemContainer",
        "5, fifthClickItemContainer",
        "6, sixthClickItemContainer",
        "7, allButOneItemContainer",
        "8, modifiableXItemContainer"
    })
    void itemContainerContextMenuEvent_dispatchesToCorrectHandler(int type, String handlerName) {
        AtomicInteger capturedType = new AtomicInteger(-1);
        PluginContext plugin = new PluginContext() {
            @Override
            protected boolean firstClickItemContainer(Player player, ItemContainerContextMenuEvent event) {
                capturedType.set(1);
                return true;
            }
            @Override
            protected boolean secondClickItemContainer(Player player, ItemContainerContextMenuEvent event) {
                capturedType.set(2);
                return true;
            }
            @Override
            protected boolean thirdClickItemContainer(Player player, ItemContainerContextMenuEvent event) {
                capturedType.set(3);
                return true;
            }
            @Override
            protected boolean fourthClickItemContainer(Player player, ItemContainerContextMenuEvent event) {
                capturedType.set(4);
                return true;
            }
            @Override
            protected boolean fifthClickItemContainer(Player player, ItemContainerContextMenuEvent event) {
                capturedType.set(5);
                return true;
            }
            @Override
            protected boolean sixthClickItemContainer(Player player, ItemContainerContextMenuEvent event) {
                capturedType.set(6);
                return true;
            }
            @Override
            protected boolean allButOneItemContainer(Player player, ItemContainerContextMenuEvent event) {
                capturedType.set(7);
                return true;
            }
            @Override
            protected boolean modifiableXItemContainer(Player player, ItemContainerContextMenuEvent event) {
                capturedType.set(8);
                return true;
            }
        };
        dataBus.subscribe(plugin);
        boolean handled = dataBus.publish(player, new ItemContainerContextMenuEvent(type, 1, 0, 1));
        assertTrue(handled);
        assertEquals(type, capturedType.get());
    }

    @Test
    void dropItemEvent_dispatchesToOnDropItem() {
        AtomicReference<DropItemEvent> captured = new AtomicReference<>();
        PluginContext plugin = new PluginContext() {
            @Override
            protected boolean onDropItem(Player player, DropItemEvent event) {
                captured.set(event);
                return true;
            }
        };
        dataBus.subscribe(plugin);
        Item item = new Item(995, 100);
        Position pos = new Position(3200, 3200);
        boolean handled = dataBus.publish(player, new DropItemEvent(item, 0, pos));
        assertTrue(handled);
        assertSame(item, captured.get().getItem());
        assertEquals(pos, captured.get().getPosition());
    }

    @Test
    void pickupItemEvent_dispatchesToOnPickupItem() {
        AtomicReference<PickupItemEvent> captured = new AtomicReference<>();
        PluginContext plugin = new PluginContext() {
            @Override
            protected boolean onPickupItem(Player player, PickupItemEvent event) {
                captured.set(event);
                return true;
            }
        };
        dataBus.subscribe(plugin);
        GroundItem groundItem = mock(GroundItem.class);
        Position pos = new Position(3200, 3200);
        when(groundItem.getPosition()).thenReturn(pos);
        boolean handled = dataBus.publish(player, new PickupItemEvent(groundItem));
        assertTrue(handled);
        assertSame(groundItem, captured.get().getGroundItem());
    }

    @Test
    void movementEvent_dispatchesToOnMovement() {
        AtomicReference<MovementEvent> captured = new AtomicReference<>();
        PluginContext plugin = new PluginContext() {
            @Override
            protected boolean onMovement(Player player, MovementEvent event) {
                captured.set(event);
                return true;
            }
        };
        dataBus.subscribe(plugin);
        Position dest = new Position(3200, 3200);
        boolean handled = dataBus.publish(player, new MovementEvent(dest));
        assertTrue(handled);
        assertEquals(dest, captured.get().getDestination());
    }

    @Test
    void unhandledEvent_returnsFalse() {
        PluginContext plugin = new PluginContext() {};
        dataBus.subscribe(plugin);
        boolean handled = dataBus.publish(player, new ButtonClickEvent(99));
        assertFalse(handled);
    }

    @Test
    void exceptionInHandler_doesNotCrashChain() {
        PluginContext throwing = new PluginContext() {
            @Override
            protected boolean onClick(Player player, int button) {
                throw new RuntimeException("test exception");
            }
        };
        AtomicBoolean secondCalled = new AtomicBoolean(false);
        PluginContext second = new PluginContext() {
            @Override
            protected boolean onClick(Player player, int button) {
                secondCalled.set(true);
                return true;
            }
        };
        dataBus.subscribe(throwing);
        dataBus.subscribe(second);
        boolean handled = dataBus.publish(player, new ButtonClickEvent(42));
        assertTrue(handled);
        assertFalse(secondCalled.get());
    }

    @Test
    void shortCircuit_stopsAfterFirstHandler() {
        AtomicInteger callCount = new AtomicInteger(0);
        PluginContext plugin = new PluginContext() {
            @Override
            protected boolean onClick(Player player, int button) {
                callCount.incrementAndGet();
                return true;
            }
        };
        dataBus.subscribe(plugin);
        dataBus.subscribe(plugin);
        dataBus.publish(player, new ButtonClickEvent(42));
        assertEquals(1, callCount.get());
    }

    @SuppressWarnings("unchecked")
    private static <T> Set<T> drainStaticSet(Class<?> clazz, String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        Set<T> set = (Set<T>) field.get(null);
        Set<T> saved = new HashSet<>(set);
        set.clear();
        return saved;
    }
}