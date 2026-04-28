package folk.sisby.antique_atlas.reloader;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.AntiqueAtlasConfig;
import folk.sisby.antique_atlas.TerrainTileProvider;
import folk.sisby.antique_atlas.TileElevation;
import folk.sisby.antique_atlas.TileTexture;
import folk.sisby.antique_atlas.util.ForgeTags;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BiomeTileProviders extends SimpleJsonResourceReloadListener<JsonElement> implements IdentifiableResourceReloadListener {
	public static final BiomeTileProviders INSTANCE = new BiomeTileProviders();
	public static final Identifier ID = AntiqueAtlas.id("tile_provider/biome");

	public static BiomeTileProviders getInstance() {
		return INSTANCE;
	}

	protected final Map<Identifier, TerrainTileProvider> tileProviders = new HashMap<>();
	protected final Map<Identifier, Identifier> biomeFallbacks = new HashMap<>();
	protected boolean hasFallbacks = false;

	public BiomeTileProviders() {
		super(ExtraCodecs.JSON, FileToIdConverter.json("atlas/biome"));
	}

	public TerrainTileProvider getTileProvider(Identifier providerId) {
		return tileProviders.getOrDefault(providerId, tileProviders.getOrDefault(biomeFallbacks.get(providerId), AntiqueAtlas.CONFIG.fallbackFailHandling == AntiqueAtlasConfig.FallbackHandling.PLAINS && !providerId.equals(Biomes.PLAINS.identifier()) ? getTileProvider(Biomes.PLAINS.identifier()) : TerrainTileProvider.DEFAULT));
	}

	/**
	 * Register fallbacks for any biomes present in the client world that don't have explicit sets.
	 * Doing this on world join catches data-biomes that might not be registered in other worlds.
	 */
	public void registerFallbacks(Registry<Biome> biomeRegistry) {
		for (Biome biome : biomeRegistry) {
			Identifier biomeId = biomeRegistry.getKey(biome);
			if (tileProviders.containsKey(biomeId)) continue;
			Identifier fallbackBiome = getFallbackBiome(biomeRegistry.wrapAsHolder(biome));
			if (fallbackBiome != null && tileProviders.containsKey(fallbackBiome)) {
				biomeFallbacks.put(biomeId, fallbackBiome);
				AntiqueAtlas.LOGGER.info("[Antique Atlas] Set fallback biome for {} to {}. You can set a more fitting texture using a resource pack!", biomeId, fallbackBiome);
			} else if (fallbackBiome != null) {
				AntiqueAtlas.LOGGER.error("[Antique Atlas] Fallback biome for {} is {}, which has no defined tile provider.", biomeId, fallbackBiome);
			} else {
				AntiqueAtlas.LOGGER.warn("[Antique Atlas] No fallback could be found for {}. This shouldn't happen! This means the biome is not in ANY conventional or vanilla tag on the client!", biomeId);
				if (AntiqueAtlas.CONFIG.fallbackFailHandling == AntiqueAtlasConfig.FallbackHandling.CRASH) throw new IllegalStateException("Antique Atlas fallback biome registration failed! Fix the missing biome or change fallbackFailHandling in antique_atlas.toml");
			}
		}
		hasFallbacks = true;
	}

	public void clearFallbacks() {
		hasFallbacks = false;
		biomeFallbacks.clear();
	}

	public boolean hasFallbacks() {
		return hasFallbacks;
	}

	public static Identifier getFallbackBiome(Holder<Biome> biome) {
		if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_VOID) || biome.is(ConventionalBiomeTags.IS_VOID) || biome.is(ForgeTags.Biomes.IS_VOID)) {
			return Biomes.THE_VOID.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_END) || biome.is(BiomeTags.IS_END) || biome.is(ConventionalBiomeTags.IS_END) || biome.is(ConventionalBiomeTags.IS_OUTER_END_ISLAND)) {
			if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_VEGETATION_SPARSE) || biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_VEGETATION_DENSE) || biome.is(ConventionalBiomeTags.IS_VEGETATION_DENSE) || biome.is(ConventionalBiomeTags.IS_VEGETATION_SPARSE) || biome.is(ForgeTags.Biomes.IS_LUSH)) return Biomes.END_HIGHLANDS.identifier();
			return Biomes.END_BARRENS.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_NETHER_FOREST) || biome.is(ConventionalBiomeTags.IS_NETHER_FOREST)) {
			return Biomes.WARPED_FOREST.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_NETHER) || biome.is(BiomeTags.IS_NETHER) || biome.is(ConventionalBiomeTags.IS_NETHER)) {
			return Biomes.SOUL_SAND_VALLEY.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_SWAMP) || biome.is(ConventionalBiomeTags.IS_SWAMP) || biome.is(ForgeTags.Biomes.IS_SWAMP)) {
			return Biomes.SWAMP.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_OCEAN) || biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN) || biome.is(ConventionalBiomeTags.IS_DEEP_OCEAN) || biome.is(ConventionalBiomeTags.IS_OCEAN) || biome.is(ConventionalBiomeTags.IS_SHALLOW_OCEAN) || biome.is(BiomeTags.IS_RIVER) || biome.is(ConventionalBiomeTags.IS_RIVER) || biome.is(ConventionalBiomeTags.IS_AQUATIC) || biome.is(ConventionalBiomeTags.IS_AQUATIC_ICY) || biome.is(ForgeTags.Biomes.IS_WATER)) {
			if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_AQUATIC_ICY) || biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_ICY) || biome.is(ConventionalBiomeTags.IS_ICY) || biome.is(ConventionalBiomeTags.IS_AQUATIC_ICY)) return Biomes.FROZEN_RIVER.identifier();
			return Biomes.RIVER.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_STONY_SHORES) || biome.is(ConventionalBiomeTags.IS_STONY_SHORES)) {
			return Biomes.STONY_SHORE.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_BEACH) || biome.is(BiomeTags.IS_BEACH) || biome.is(ConventionalBiomeTags.IS_BEACH)) {
			return Biomes.BEACH.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_JUNGLE_TREE) || biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_JUNGLE) || biome.is(BiomeTags.IS_JUNGLE) || biome.is(ConventionalBiomeTags.IS_JUNGLE) || biome.is(ConventionalBiomeTags.IS_JUNGLE_TREE)) {
			return Biomes.JUNGLE.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_FLOWER_FOREST) || biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_FLORAL) || biome.is(ConventionalBiomeTags.IS_FLOWER_FOREST) || biome.is(ConventionalBiomeTags.IS_FLORAL)) {
			return Biomes.FLOWER_FOREST.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_SAVANNA_TREE) || biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_SAVANNA) || biome.is(BiomeTags.IS_SAVANNA) || biome.is(ConventionalBiomeTags.IS_SAVANNA) || biome.is(ConventionalBiomeTags.IS_SAVANNA_TREE)) {
			return Biomes.SAVANNA.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_BADLANDS) || biome.is(BiomeTags.IS_BADLANDS) || biome.is((ConventionalBiomeTags.IS_BADLANDS)) || biome.is((ConventionalBiomeTags.IS_BADLANDS))) {
			return Biomes.BADLANDS.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_CONIFEROUS_TREE) || biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_TAIGA) || biome.is(ConventionalBiomeTags.IS_CONIFEROUS_TREE) || biome.is(ForgeTags.Biomes.IS_CONIFEROUS) || biome.is(BiomeTags.IS_TAIGA) || biome.is(ConventionalBiomeTags.IS_TAIGA)) {
			if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_SNOWY) || biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_ICY) || biome.is(ConventionalBiomeTags.IS_ICY) || biome.is(ConventionalBiomeTags.IS_SNOWY)) return Biomes.SNOWY_TAIGA.identifier();
			return Biomes.TAIGA.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_OLD_GROWTH)) {
			return Biomes.BIRCH_FOREST.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_BIRCH_FOREST) || biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_DECIDUOUS_TREE) || biome.is(ConventionalBiomeTags.IS_BIRCH_FOREST) || biome.is(ConventionalBiomeTags.IS_DECIDUOUS_TREE)) {
			return Biomes.BIRCH_FOREST.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_FOREST) || biome.is(ConventionalBiomeTags.IS_FOREST)) {
			return Biomes.FOREST.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_SNOWY_PLAINS) || biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_PLAINS) || biome.is(ConventionalBiomeTags.IS_PLAINS) || biome.is(ConventionalBiomeTags.IS_SNOWY_PLAINS) || biome.is(ForgeTags.Biomes.IS_PLAINS) || biome.is(ConventionalBiomeTags.IS_SNOWY) || biome.is(ForgeTags.Biomes.IS_SNOWY)) {
			if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_ICY) || biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_SNOWY_PLAINS) || biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_SNOWY) || biome.is(ConventionalBiomeTags.IS_SNOWY_PLAINS) || biome.is(ConventionalBiomeTags.IS_ICY) || biome.is(ConventionalBiomeTags.IS_SNOWY)) return Biomes.SNOWY_PLAINS.identifier();
			return Biomes.PLAINS.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_WASTELAND) || biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_DEAD) || biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_DESERT) || biome.is(ConventionalBiomeTags.IS_DESERT) || biome.is(ConventionalBiomeTags.IS_WASTELAND) || biome.is(ConventionalBiomeTags.IS_DEAD) || biome.is(ForgeTags.Biomes.IS_SANDY) || biome.is(ForgeTags.Biomes.IS_DESERT) || biome.is(ForgeTags.Biomes.IS_DEAD) || biome.is(ForgeTags.Biomes.IS_WASTELAND)) {
			return Biomes.DESERT.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_ICY) || biome.is(ConventionalBiomeTags.IS_ICY)) {
			return Biomes.FROZEN_OCEAN.identifier();
		} else if (biome.is(ForgeTags.Biomes.IS_PLATEAU)) {
			return Biomes.MEADOW.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_WINDSWEPT) || biome.is(ConventionalBiomeTags.IS_WINDSWEPT) || biome.is(ConventionalBiomeTags.IS_WINDSWEPT)) {
			return Biomes.WINDSWEPT_HILLS.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_MOUNTAIN_PEAK) || biome.is(ConventionalBiomeTags.IS_MOUNTAIN_PEAK) || biome.is(ForgeTags.Biomes.IS_PEAK)) {
			return Biomes.JAGGED_PEAKS.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_MOUNTAIN_SLOPE) || biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_MOUNTAIN) || biome.is(BiomeTags.IS_MOUNTAIN) || biome.is(ConventionalBiomeTags.IS_MOUNTAIN) || biome.is(ConventionalBiomeTags.IS_MOUNTAIN_SLOPE) || biome.is(ForgeTags.Biomes.IS_SLOPE) || biome.is(ForgeTags.Biomes.IS_MOUNTAIN)) {
			return Biomes.STONY_PEAKS.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_MUSHROOM) || biome.is(ConventionalBiomeTags.IS_MUSHROOM) || biome.is(ForgeTags.Biomes.IS_MUSHROOM)) {
			return Biomes.MUSHROOM_FIELDS.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_HILL) || biome.is(BiomeTags.IS_HILL)) {
			return Biomes.WINDSWEPT_GRAVELLY_HILLS.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_UNDERGROUND) || biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_CAVE) || biome.is(ConventionalBiomeTags.IS_CAVE) || biome.is(ConventionalBiomeTags.IS_UNDERGROUND) || biome.is(ForgeTags.Biomes.IS_UNDERGROUND) || biome.is(ForgeTags.Biomes.IS_CAVE)) {
			return Biomes.DRIPSTONE_CAVES.identifier();
		} else if (biome.is(ForgeTags.Biomes.IS_SPOOKY)) {
			return Biomes.DARK_FOREST.identifier();
		} else if (biome.is(ForgeTags.Biomes.IS_MAGICAL)) {
			return Biomes.MUSHROOM_FIELDS.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_VEGETATION_DENSE) || biome.is(ConventionalBiomeTags.IS_VEGETATION_DENSE) || biome.is(ForgeTags.Biomes.IS_DENSE)) {
			return Biomes.FOREST.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_VEGETATION_SPARSE) || biome.is(ConventionalBiomeTags.IS_VEGETATION_SPARSE) || biome.is(ForgeTags.Biomes.IS_SPARSE)) {
			return Biomes.PLAINS.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_HOT) || biome.is(ConventionalBiomeTags.IS_HOT) || biome.is(ForgeTags.Biomes.IS_HOT)) {
			return Biomes.DESERT.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_COLD) || biome.is(ConventionalBiomeTags.IS_COLD) || biome.is(ForgeTags.Biomes.IS_COLD)) {
			return Biomes.SNOWY_PLAINS.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_TEMPERATE) || biome.is(ConventionalBiomeTags.IS_TEMPERATE)) {
			return Biomes.PLAINS.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_DRY) || biome.is(ConventionalBiomeTags.IS_DRY) || biome.is(ForgeTags.Biomes.IS_DRY)) {
			return Biomes.BADLANDS.identifier();
		} else if (biome.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_WET) || biome.is(ConventionalBiomeTags.IS_WET) || biome.is(ForgeTags.Biomes.IS_WET)) {
			return Biomes.SWAMP.identifier();
		}
		return null;
	}

	public static TileTexture getTexture(Map<Identifier, TileTexture> textures, Identifier id) {
		if (textures.containsKey(id)) {
			return textures.get(id);
		} else {
			throw new IllegalStateException("texture %s is not present!".formatted(id));
		}
	}

	public static @Nullable List<TileTexture> resolveTextureJson(Map<Identifier, TileTexture> textures, JsonElement textureJson) {
		if (textureJson instanceof JsonPrimitive texturePrimitive && texturePrimitive.isString()) {
			return List.of(getTexture(textures, Identifier.tryParse(texturePrimitive.getAsString())));
		} else if (textureJson instanceof JsonArray textureArray) {
			return textureArray.asList().stream().map(je -> getTexture(textures, Identifier.tryParse(je.getAsString()))).toList();
		} else if (textureJson instanceof JsonObject textureObject && textureObject.keySet().stream().allMatch(k -> textureObject.get(k) instanceof JsonPrimitive jp && jp.isNumber())) {
			Multiset<TileTexture> outList = HashMultiset.create();
			textureObject.entrySet().forEach(e -> outList.add(getTexture(textures, Identifier.tryParse(e.getKey())), e.getValue().getAsInt()));
			return outList.stream().toList();
		}
		return null;
	}

	@Override
	protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {
		AntiqueAtlas.LOGGER.info("[Antique Atlas] Reloading Biome Tile Providers...");
		Map<Identifier, TileTexture> textures = TileTextures.getInstance().getTextures();
		Set<TileTexture> unusedTextures = new HashSet<>(textures.values().stream().filter(t -> t.id().getPath().startsWith("biome")).toList());
		Map<Identifier, Identifier> providerParents = new HashMap<>();
		for (Map.Entry<Identifier, JsonElement> fileEntry : prepared.entrySet()) {
			Identifier fileId = fileEntry.getKey();
			try {
				JsonObject fileJson = fileEntry.getValue().getAsJsonObject();
				if (fileJson.has("parent")) {
					Identifier parentId = Identifier.tryParse(fileJson.getAsJsonPrimitive("parent").getAsString());
					providerParents.put(fileId, parentId);
					continue;
				}
				JsonElement textureJson = fileJson.get("textures");
				List<TileTexture> defaultTextures = resolveTextureJson(textures, textureJson);
				if (defaultTextures != null) {
					defaultTextures.forEach(unusedTextures::remove);
					tileProviders.put(fileId, new TerrainTileProvider(fileId, defaultTextures));
				} else {
					JsonObject textureObject = textureJson.getAsJsonObject();
					Map<TileElevation, List<TileTexture>> textureElevations = new HashMap<>();
					Set<TileElevation> skippedElevations = new HashSet<>();
					List<TileTexture> elevationTextures = null;
					for (TileElevation elevation : TileElevation.values()) {
						if (textureObject.has(elevation.getName())) {
							elevationTextures = resolveTextureJson(textures, textureObject.get(elevation.getName()));
							if (elevationTextures == null) throw new IllegalStateException("Malformed object %s in textures object!".formatted(elevation.getName()));
							elevationTextures.forEach(unusedTextures::remove);
							textureElevations.put(elevation, elevationTextures);
							for (TileElevation skipped : skippedElevations) {
								textureElevations.put(skipped, elevationTextures);
							}
							skippedElevations.clear();
						} else {
							skippedElevations.add(elevation);
						}
					}
					if (textureElevations.isEmpty()) {
						throw new IllegalStateException("No elevation keys were found in the textures object!");
					}
					for (TileElevation elevation : skippedElevations) {
						textureElevations.put(elevation, elevationTextures);
					}
					tileProviders.put(fileId, new TerrainTileProvider(fileId, textureElevations));
				}
			} catch (Exception e) {
				AntiqueAtlas.LOGGER.error("[Antique Atlas] Error reading biome tile provider {}!", fileId, e);
			}
		}
		providerParents.forEach((id, parentId) -> {
			if (tileProviders.containsKey(parentId)) {
				tileProviders.put(id, tileProviders.get(parentId));
			} else {
				AntiqueAtlas.LOGGER.error("[Antique Atlas] Error reading biome tile provider {}!", id, new IllegalStateException("Parent id %s doesn't exist".formatted(parentId)));
			}
		});

		for (TileTexture texture : unusedTextures) {
			if (texture.displayId().startsWith("test") || texture.displayId().startsWith("base")) continue;
			AntiqueAtlas.LOGGER.warn("[Antique Atlas] Tile texture {} isn't referenced by any biome tile provider!", texture.displayId());
		}
	}

	@Override
	public Identifier getFabricId() {
		return ID;
	}

	@Override
	public Collection<Identifier> getFabricDependencies() {
		return List.of(TileTextures.ID);
	}
}
