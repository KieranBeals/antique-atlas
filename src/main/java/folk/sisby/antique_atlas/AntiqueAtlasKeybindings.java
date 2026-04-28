package folk.sisby.antique_atlas;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;


public class AntiqueAtlasKeybindings {
	public static final KeyMapping ATLAS_KEYMAPPING = new KeyMapping("key.antique_atlas.open", InputConstants.Type.KEYSYM, 77, KeyMapping.Category.register(AntiqueAtlas.id("category")));

	public static void init() {
		KeyMappingHelper.registerKeyMapping(ATLAS_KEYMAPPING);
		ClientTickEvents.END_CLIENT_TICK.register(AntiqueAtlasKeybindings::onClientTick);
	}

	public static void onClientTick(Minecraft client) {
		while (ATLAS_KEYMAPPING.consumeClick()) AntiqueAtlas.openAtlasScreen();
	}
}
