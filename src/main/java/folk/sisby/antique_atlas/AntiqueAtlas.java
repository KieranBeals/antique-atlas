package folk.sisby.antique_atlas;

import folk.sisby.antique_atlas.gui.AtlasScreen;
import folk.sisby.antique_atlas.gui.core.ScreenState;
import folk.sisby.antique_atlas.reloader.BiomeTileProviders;
import folk.sisby.antique_atlas.reloader.MarkerTextures;
import folk.sisby.antique_atlas.reloader.StructureTileProviders;
import folk.sisby.antique_atlas.reloader.TileTextures;
import folk.sisby.surveyor.PlayerSummary;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.client.SurveyorClientEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AntiqueAtlas implements ClientModInitializer {
	public static final String ID = "antique_atlas";
	public static final String NAME = "Antique Atlas";

	public static final Logger LOGGER = LogManager.getLogger(NAME);

	public static final AntiqueAtlasConfig CONFIG = AntiqueAtlasConfig.createToml(FabricLoader.getInstance().getConfigDir(), "", "antique-atlas", AntiqueAtlasConfig.class);
	public static final ScreenState<AtlasScreen> lastState = new ScreenState<>();

	public static final List<String> ATLAS_NAMES = List.of(
		"Antique Atlas",
		"Atlas antiguo",
		"Antyczny atlas",
		"Античный Атлас"
	);

	public static Identifier id(String path) {
		return path.contains(":") ? Identifier.tryParse(path) : Identifier.fromNamespaceAndPath(ID, path);
	}

	public static ItemStack getHandheldAtlas() {
		ItemStack stack = Items.BOOK.getDefaultInstance().copy();
		stack.set(DataComponents.ITEM_NAME, Component.translatable("item.antique_atlas.atlas"));
		stack.set(DataComponents.ITEM_MODEL, AntiqueAtlas.id("atlas"));
		stack.set(DataComponents.LORE, new ItemLore(List.of(
			Component.translatable("item.antique_atlas.atlas.lore").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false)),
			Component.translatable("item.antique_atlas.atlas.hint", Component.translatable("item.antique_atlas.atlas")).setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false))
		)));
		return stack;
	}

	public static AtlasScreen openAtlasScreen() {
		if (Minecraft.getInstance().screen == null && (!AntiqueAtlas.CONFIG.requireItem || (Minecraft.getInstance().player != null && AntiqueAtlas.hasHandheldAtlas(Minecraft.getInstance().player)))) {
			AtlasScreen screen = new AtlasScreen();
			screen.init();
			screen.prepareToOpen();
			screen.tick();
			Minecraft.getInstance().setScreen(screen);
			return screen;
		}
		return null;
	}

	public static boolean isHandheldAtlas(ItemStack stack) {
		return stack.is(Items.BOOK) && ATLAS_NAMES.stream().anyMatch(n -> stack.getHoverName().getString().toLowerCase().contains(n.toLowerCase()));
	}

	public static boolean hasHandheldAtlas(Player player) {
		if (isHandheldAtlas(player.getOffhandItem())) return true;
		for (ItemStack itemStack : player.getInventory().getNonEquipmentItems()) {
			if (isHandheldAtlas(itemStack)) {
				return true;
			}
		}
		return false;
	}

	public static Map<UUID, PlayerSummary> getOrderedFriends() {
		Map<UUID, PlayerSummary> friends = SurveyorClient.getFriends();
		PlayerSummary playerSummary = friends.remove(SurveyorClient.getClientUuid());
		Map<UUID, PlayerSummary> orderedFriends = new LinkedHashMap<>(friends);
		if (playerSummary != null) orderedFriends.put(SurveyorClient.getClientUuid(), playerSummary);
		return orderedFriends;
	}

	@Override
	public void onInitializeClient() {
		AntiqueAtlasKeybindings.init();
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(TileTextures.getInstance());
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(StructureTileProviders.getInstance());
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(BiomeTileProviders.getInstance());
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(MarkerTextures.getInstance());

		SurveyorClientEvents.Register.terrainUpdated(id("world_data"), (s, k) -> WorldAtlasData.getOrCreate(s.dimension()).onTerrainUpdated(s, k));
		SurveyorClientEvents.Register.structuresAdded(id("world_data"), (s, k) -> WorldAtlasData.getOrCreate(s.dimension()).onStructuresAdded(s, k));
		SurveyorClientEvents.Register.landmarksAdded(id("world_data"), (s, k) -> WorldAtlasData.getOrCreate(s.dimension()).onLandmarksAdded(s, k));
		SurveyorClientEvents.Register.landmarksRemoved(id("world_data"), (s, k) -> WorldAtlasData.getOrCreate(s.dimension()).onLandmarksRemoved(s, k));
		ClientTickEvents.END_LEVEL_TICK.register((w -> SurveyorClient.getSummaries(Minecraft.getInstance().getConnection()).values().forEach(s -> WorldAtlasData.getOrCreate(s.dimension()).tick(s))));
		CommonLifecycleEvents.TAGS_LOADED.register(((manager, client) -> BiomeTileProviders.getInstance().registerFallbacks(manager.lookupOrThrow(Registries.BIOME))));
		ClientPlayConnectionEvents.DISCONNECT.register(((handler, client) -> BiomeTileProviders.getInstance().clearFallbacks()));
		ClientPlayConnectionEvents.DISCONNECT.register(((handler, client) -> WorldAtlasData.WORLDS.clear()));

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e -> e.insertAfter(Items.MAP, getHandheldAtlas()));

		WorldSummary.enableTerrain();
		WorldSummary.enableStructures();
		WorldSummary.enableLandmarks();

		FabricLoader.getInstance().getModContainer(ID).ifPresent(c -> ResourceManagerHelper.registerBuiltinResourcePack(id("shader_patch"), c, Component.nullToEmpty("Shader Patch"), ResourcePackActivationType.NORMAL));
	}
}
