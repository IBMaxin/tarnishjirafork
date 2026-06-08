package scripts;

import com.osroyale.fs.cache.FileSystem;
import com.osroyale.fs.cache.decoder.CacheNpcDefinition;

public class CacheNpcDump {
    public static void main(String[] args) throws Exception {
        FileSystem fs = FileSystem.create("game-server/data/cache");
        CacheNpcDefinition.unpackConfig(fs.archive(0, 2)); // config index, npc archive
        int max = 20000;
        for (int id = 0; id < max; id++) {
            try {
                CacheNpcDefinition def = CacheNpcDefinition.lookup(id);
                if (def == null) continue;
                String name = def.name != null ? def.name.replace('\n',' ').trim() : "";
                if (name.isEmpty()) continue;
                int combat = def.combatLevel;
                System.out.println(id + "\t" + combat + "\t" + name);
            } catch (Exception ignored) {
            }
        }
    }
}
