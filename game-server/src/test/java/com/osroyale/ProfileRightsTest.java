package com.osroyale;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.osroyale.game.world.entity.mob.player.PlayerRight;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public final class ProfileRightsTest {

    @Test
    public void zezimaIsOwner() throws IOException {
        JsonObject profile = readJson("data/profile/save/Zezima.json");
        assertEquals("Zezima", profile.get("username").getAsString());
        assertEquals(PlayerRight.OWNER, PlayerRight.valueOf(profile.get("player-rights").getAsString()));
    }

    @Test
    public void oakIsAdministratorButNotOwner() throws IOException {
        JsonObject profile = readJson("data/profile/save/Oak.json");
        PlayerRight right = PlayerRight.valueOf(profile.get("player-rights").getAsString());

        assertEquals("Oak", profile.get("username").getAsString());
        assertEquals(PlayerRight.ADMINISTRATOR, right);
        assertNotEquals(PlayerRight.OWNER, right);
    }

    @Test
    public void worldProfileListMatchesSavedRights() throws IOException {
        JsonObject profiles = readJson("data/profile/world_profile_list.json");

        assertEquals("OWNER", profiles.getAsJsonObject("Zezima").get("rank").getAsString());
        assertEquals("ADMINISTRATOR", profiles.getAsJsonObject("Oak").get("rank").getAsString());
    }

    private static JsonObject readJson(String path) throws IOException {
        return JsonParser.parseString(Files.readString(Path.of(path))).getAsJsonObject();
    }

}
