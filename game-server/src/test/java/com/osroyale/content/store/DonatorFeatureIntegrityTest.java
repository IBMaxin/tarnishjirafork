package com.osroyale.content.store;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Validates donator store integrity.
 *
 * <p>Checks that:
 * <ul>
 *   <li>Donator stores exist in stores.json</li>
 *   <li>They use DONATOR_POINTS currency</li>
 *   <li>They have items defined</li>
 * </ul>
 */
public final class DonatorFeatureIntegrityTest {

    private static final Path STORES_PATH = Path.of("data/def/store/stores.json");

    private static List<StoreJson> donatorStores;

    @BeforeAll
    public static void setup() throws Exception {
        String storeJson = Files.readString(STORES_PATH);
        var storeArray = new com.google.gson.JsonParser().parse(storeJson).getAsJsonArray();

        donatorStores = new ArrayList<>();
        for (var element : storeArray) {
            var obj = element.getAsJsonObject();
            String name = obj.get("name").getAsString();
            if (!name.toLowerCase().contains("donator")) {
                continue;
            }
            String currency = obj.get("currency").getAsString();
            var itemsArray = obj.getAsJsonArray("items");
            List<StoreItemJson> items = new ArrayList<>(itemsArray.size());
            for (var itemEl : itemsArray) {
                var itemObj = itemEl.getAsJsonObject();
                items.add(new StoreItemJson(
                        itemObj.get("id").getAsInt(),
                        itemObj.get("amount").getAsInt(),
                        itemObj.get("value").getAsInt()));
            }
            donatorStores.add(new StoreJson(name, currency, items));
        }
    }

    @Test
    public void donatorStoresExist() {
        if (donatorStores.isEmpty()) {
            fail("No donator stores found in stores.json. Expected at least 'Donator Store'.");
        }
    }

    @Test
    public void donatorStoresUseDonatorPoints() {
        List<String> failures = new ArrayList<>();
        for (StoreJson store : donatorStores) {
            if (!"DONATOR_POINTS".equals(store.currency)) {
                failures.add(String.format(
                        "Donator store '%s' uses '%s' instead of DONATOR_POINTS",
                        store.name, store.currency));
            }
        }
        if (!failures.isEmpty()) {
            fail("Donator store currency issues (" + failures.size() + "):\n  "
                    + String.join("\n  ", failures));
        }
    }

    @Test
    public void donatorStoresHaveItems() {
        List<String> failures = new ArrayList<>();
        for (StoreJson store : donatorStores) {
            if (store.items.isEmpty()) {
                failures.add("Donator store '" + store.name + "' has no items.");
            }
        }
        if (!failures.isEmpty()) {
            fail("Empty donator stores (" + failures.size() + "):\n  "
                    + String.join("\n  ", failures));
        }
    }

    // ── Helper records ──────────────────────────────────────────

    private record StoreJson(String name, String currency, List<StoreItemJson> items) {}

    private record StoreItemJson(int id, int amount, int value) {}
}
