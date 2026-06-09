package com.osroyale.content.store;

import com.osroyale.content.store.currency.CurrencyType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Validates the integrity of every shop in stores.json.
 *
 * <p>Checks that:
 * <ul>
 *   <li>All item IDs exist in item definitions</li>
 *   <li>Item amounts are positive</li>
 *   <li>Shop names are unique</li>
 *   <li>Currency types are recognized by the server</li>
 *   <li>Sell types are valid</li>
 * </ul>
 */
public final class FeatureShopIntegrityTest {

    private static final Path STORES_PATH = Path.of("data/def/store/stores.json");
    private static final Path ITEM_DEFS_PATH = Path.of("data/def/item/item_definitions.json");

    private static Set<Integer> validItemIds;
    private static List<StoreJson> stores;

    @BeforeAll
    public static void setup() throws Exception {
        // Load item definitions
        String itemJson = Files.readString(ITEM_DEFS_PATH);
        var itemArray = new com.google.gson.JsonParser().parse(itemJson).getAsJsonArray();
        validItemIds = new HashSet<>(itemArray.size());
        for (var element : itemArray) {
            validItemIds.add(element.getAsJsonObject().get("id").getAsInt());
        }

        // Load stores
        String storeJson = Files.readString(STORES_PATH);
        var storeArray = new com.google.gson.JsonParser().parse(storeJson).getAsJsonArray();
        stores = new ArrayList<>(storeArray.size());
        for (var element : storeArray) {
            var obj = element.getAsJsonObject();
            String name = obj.get("name").getAsString();
            String currency = obj.get("currency").getAsString();
            String sellType = obj.get("sellType").getAsString();
            boolean restock = obj.get("restock").getAsBoolean();

            var itemsArray = obj.getAsJsonArray("items");
            List<StoreItemJson> items = new ArrayList<>(itemsArray.size());
            for (var itemEl : itemsArray) {
                var itemObj = itemEl.getAsJsonObject();
                int id = itemObj.get("id").getAsInt();
                int amount = itemObj.get("amount").getAsInt();
                int value = itemObj.has("value") ? itemObj.get("value").getAsInt() : 0;
                items.add(new StoreItemJson(id, amount, value));
            }

            stores.add(new StoreJson(name, currency, sellType, restock, items));
        }
    }

    @Test
    public void allShopItemIdsExist() {
        List<String> failures = new ArrayList<>();

        for (StoreJson store : stores) {
            for (StoreItemJson item : store.items) {
                if (!validItemIds.contains(item.id)) {
                    failures.add(String.format(
                            "Shop '%s' has unknown item ID %d", store.name, item.id));
                }
            }
        }

        if (!failures.isEmpty()) {
            fail("Unknown item IDs found (" + failures.size() + "):\n  "
                    + String.join("\n  ", failures));
        }
    }

    @Test
    public void allItemAmountsPositive() {
        List<String> failures = new ArrayList<>();

        for (StoreJson store : stores) {
            for (StoreItemJson item : store.items) {
                if (item.amount <= 0) {
                    failures.add(String.format(
                            "Shop '%s' item %d has non-positive amount: %d",
                            store.name, item.id, item.amount));
                }
            }
        }

        if (!failures.isEmpty()) {
            fail("Non-positive item amounts found (" + failures.size() + "):\n  "
                    + String.join("\n  ", failures));
        }
    }

    @Test
    public void allShopNamesUnique() {
        List<String> duplicates = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (StoreJson store : stores) {
            if (!seen.add(store.name)) {
                duplicates.add(store.name);
            }
        }

        if (!duplicates.isEmpty()) {
            fail("Duplicate shop names (" + duplicates.size() + "):\n  "
                    + String.join("\n  ", duplicates));
        }
    }

    @Test
    public void allCurrenciesRecognized() {
        Set<String> validCurrencies = new HashSet<>();
        for (CurrencyType type : CurrencyType.values()) {
            validCurrencies.add(type.name());
        }

        List<String> failures = new ArrayList<>();
        for (StoreJson store : stores) {
            if (!validCurrencies.contains(store.currency)) {
                failures.add(String.format(
                        "Shop '%s' has unknown currency: %s", store.name, store.currency));
            }
        }

        if (!failures.isEmpty()) {
            fail("Unknown currencies found (" + failures.size() + "):\n  "
                    + String.join("\n  ", failures));
        }
    }

    @Test
    public void allSellTypesValid() {
        Set<String> validSellTypes = Set.of("NONE", "ANY");
        List<String> failures = new ArrayList<>();

        for (StoreJson store : stores) {
            if (!validSellTypes.contains(store.sellType)) {
                failures.add(String.format(
                        "Shop '%s' has unknown sellType: %s", store.name, store.sellType));
            }
        }

        if (!failures.isEmpty()) {
            fail("Unknown sell types found (" + failures.size() + "):\n  "
                    + String.join("\n  ", failures));
        }
    }

    // ── Helper records ──────────────────────────────────────────

    private record StoreJson(String name, String currency, String sellType, boolean restock, List<StoreItemJson> items) {}

    private record StoreItemJson(int id, int amount, int value) {}
}

