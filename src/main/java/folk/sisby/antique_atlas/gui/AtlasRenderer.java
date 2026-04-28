package folk.sisby.antique_atlas.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.MarkerTexture;
import folk.sisby.antique_atlas.TileTexture;
import folk.sisby.antique_atlas.WorldAtlasData;
import folk.sisby.antique_atlas.gui.core.ScreenState;
import folk.sisby.antique_atlas.gui.tiles.SubTile;
import folk.sisby.antique_atlas.gui.tiles.SubTileQuartet;
import folk.sisby.antique_atlas.gui.tiles.TileRenderIterator;
import folk.sisby.antique_atlas.util.ColorUtil;
import folk.sisby.antique_atlas.util.DrawBatcher;
import folk.sisby.antique_atlas.util.DrawUtil;
import folk.sisby.antique_atlas.util.MathUtil;
import folk.sisby.antique_atlas.util.Rect;
import folk.sisby.surveyor.PlayerSummary;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.component.LandmarkComponentTypes;
import folk.sisby.surveyor.util.RegionPos;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public interface AtlasRenderer {
	Map<Identifier, AtlasOverlay> overlays = new HashMap<>();

	static void registerOverlay(Identifier id, AtlasOverlay overlay) {
		overlays.put(id, overlay);
	}

	Identifier BOOK = AntiqueAtlas.id("textures/gui/book.png");
	Identifier BOOK_FULLSCREEN = AntiqueAtlas.id("book_fullscreen");
	Identifier BOOK_FULLSCREEN_M = AntiqueAtlas.id("middle/book_fullscreen_m");
	Identifier BOOK_FULLSCREEN_R = AntiqueAtlas.id("book_fullscreen_r");
	Identifier BOOK_FRAME = AntiqueAtlas.id("textures/gui/book_frame.png");
	Identifier BOOK_FRAME_FULLSCREEN = AntiqueAtlas.id("book_frame_fullscreen");
	Identifier BOOK_FRAME_FULLSCREEN_M = AntiqueAtlas.id("middle/book_frame_fullscreen_m");
	Identifier BOOK_FRAME_FULLSCREEN_R = AntiqueAtlas.id("book_frame_fullscreen_r");
	Identifier BOOK_FRAME_NARROW = AntiqueAtlas.id("textures/gui/book_frame_narrow.png");
	Identifier BOOK_FRAME_NARROW_FULLSCREEN = AntiqueAtlas.id("book_frame_narrow_fullscreen");
	Identifier BOOK_FRAME_NARROW_FULLSCREEN_M = AntiqueAtlas.id("middle/book_frame_narrow_fullscreen_m");
	Identifier BOOK_FRAME_NARROW_FULLSCREEN_R = AntiqueAtlas.id("book_frame_narrow_fullscreen_r");
	Identifier PLAYER = AntiqueAtlas.id("textures/gui/player.png");
	Identifier ERASER = AntiqueAtlas.id("textures/gui/eraser.png");
	Identifier ICON_ADD_MARKER = AntiqueAtlas.id("textures/gui/icons/add_marker.png");
	Identifier ICON_DELETE_MARKER = AntiqueAtlas.id("textures/gui/icons/del_marker.png");
	Identifier ICON_SHOW_MARKERS = AntiqueAtlas.id("textures/gui/icons/show_markers.png");
	Identifier ICON_HIDE_MARKERS = AntiqueAtlas.id("textures/gui/icons/hide_markers.png");
	Identifier ICON_UNKNOWN = AntiqueAtlas.id("textures/gui/icons/unknown.png");
	Component TEXT_ADD_MARKER = Component.translatable("gui.antique_atlas.addMarker");
	Component TEXT_ADD_MARKER_HERE = Component.translatable("gui.antique_atlas.addMarkerHere");

	int DEFAULT_BOOK_WIDTH = 310;
	int DEFAULT_BOOK_HEIGHT = 218;
	int MAP_BORDER_WIDTH = 17;
	int MAP_BORDER_HEIGHT = 11;
	float PLAYER_ROTATION_STEPS = 16;
	int PLAYER_ICON_WIDTH = 7;
	int PLAYER_ICON_HEIGHT = 8;
	int BOOKMARK_SPACING = 2;
	int MARKER_SIZE = 32;
	int NAVIGATE_STEP = 24; // How much the map view is offset, in blocks, per click (or per tick).
	int MAX_LIGHT = 0xF000F0;

	ScreenState.State<AtlasScreen> NORMAL = new ScreenState.ToggleState<>();
	ScreenState.State<AtlasScreen> PLACING_MARKER = new ScreenState.ToggleState<>(s -> s.addMarkerBookmark);
	ScreenState.State<AtlasScreen> DELETING_MARKER = new ScreenState.ToggleState<>(s -> s.deleteMarkerBookmark, s -> s.addChild(s.eraser), s -> s.removeChild(s.eraser));
	ScreenState.State<AtlasScreen> HIDING_MARKERS = new ScreenState.ToggleState<>(s -> s.markerVisibilityBookmark, s -> {
		s.markerVisibilityBookmark.setTitle(Component.translatable("gui.antique_atlas.showMarkers"));
		s.markerVisibilityBookmark.setIconTexture(ICON_SHOW_MARKERS);
	}, s -> {
		s.clearTargetBookmarks(s.playerBookmark);
		s.markerVisibilityBookmark.setTitle(Component.translatable("gui.antique_atlas.hideMarkers"));
		s.markerVisibilityBookmark.setIconTexture(ICON_HIDE_MARKERS);
	});

	int bookX();

	int bookY();

	int bookWidth();

	int bookHeight();

	int mapWidth();

	int mapHeight();

	double mapOffsetX();

	double mapOffsetY();

	int tilePixels();

	int tileChunks();

	int mapScale();

	Player player();

	WorldAtlasData worldAtlasData();

	double getPixelsPerBlock();

	double guiScale();

	ResourceKey<Level> dim();

	default int screenXToWorldX(double screenX) {
		return screenXToWorldX(screenX, bookX(), mapOffsetX(), mapWidth(), getPixelsPerBlock());
	}

	default int screenYToWorldZ(double screenY) {
		return screenYToWorldZ(screenY, bookY(), mapOffsetY(), mapHeight(), getPixelsPerBlock());
	}

	default double worldXToScreenX(double x) {
		return worldXToScreenX(x, bookX(), mapOffsetX(), mapWidth(), getPixelsPerBlock());
	}

	default double worldZToScreenY(double z) {
		return worldZToScreenY(z, bookY(), mapOffsetY(), mapHeight(), getPixelsPerBlock());
	}

	static int screenXToWorldX(double screenX, int bookX, double mapOffsetX, int mapWidth, double pixelsPerBlock) {
		double mapX = (int) Math.round(screenX - bookX - MAP_BORDER_WIDTH);
		return (int) Math.round((mapX - (mapWidth / 2f) - mapOffsetX) / pixelsPerBlock);
	}

	static int screenYToWorldZ(double screenY, int bookY, double mapOffsetY, int mapHeight, double pixelsPerBlock) {
		double mapY = (int) Math.round(screenY - bookY - MAP_BORDER_HEIGHT);
		return (int) Math.round((mapY - (mapHeight / 2f) - mapOffsetY) / pixelsPerBlock);
	}

	static double worldXToScreenX(double x, int bookX, double mapOffsetX, int mapWidth, double pixelsPerBlock) {
		double mapX = x * pixelsPerBlock + mapOffsetX + (mapWidth / 2f);
		return mapX + bookX + MAP_BORDER_WIDTH;
	}

	static double worldZToScreenY(double z, int bookY, double mapOffsetY, int mapHeight, double pixelsPerBlock) {
		double mapY = z * pixelsPerBlock + mapOffsetY + (mapHeight / 2f);
		return mapY + bookY + MAP_BORDER_HEIGHT;
	}

	default void renderMarker(PoseStack matrices, SubmitNodeCollector submitter, Landmark landmark, MarkerTexture texture, float z, int light, BiFunction<Double, Double, Float> alphaGetter, boolean pinned, boolean hovering, float markerScale) {
		BlockPos pos = landmark.get(LandmarkComponentTypes.POS);
		Integer color = landmark.get(LandmarkComponentTypes.COLOR);
		float[] accent = color == null ? null : ColorUtil.componentsFromRgb(color);
		float tint = hovering ? 0.8f : 1.0f;

		if (pos == null) {
			Set<ChunkPos> chunks = RegionPos.regionsToChunks(landmark.getOrDefault(LandmarkComponentTypes.CHUNKS, new HashMap<>()));
			for (ChunkPos chunk : chunks) {
				double markerX = worldXToScreenX(chunk.getMinBlockX()) - bookX();
				double markerY = worldZToScreenY(chunk.getMinBlockZ()) - bookY();
				float effectiveScale = (float) (mapScale() / guiScale());
				matrices.pushPose();
				matrices.translate(markerX, markerY, 0.0);
				matrices.scale(effectiveScale, effectiveScale, 1.0F);
				int size = tilePixels() / tileChunks();
				int lineSize = tilePixels() / 16;
				if (size > 0) {
					float[] fillColor = accent == null ? ColorUtil.componentsFromRgb(0xFFFFFF) : new float[] { tint * accent[0], tint * accent[1], tint * accent[2] };
					float alpha = alphaGetter.apply(markerX, markerY);
					DrawUtil.fill(matrices, submitter, RenderTypes.textBackgroundSeeThrough(), z, light, 0, 0, size, size, 0.25F * alpha, fillColor);
					if (lineSize > 0) {
						if (!chunks.contains(new ChunkPos(chunk.x() - 1, chunk.z()))) DrawUtil.fill(matrices, submitter, RenderTypes.textBackgroundSeeThrough(), z, light, 0, 0, lineSize, size, 0.5F * alpha, fillColor);
						if (!chunks.contains(new ChunkPos(chunk.x(), chunk.z() - 1))) DrawUtil.fill(matrices, submitter, RenderTypes.textBackgroundSeeThrough(), z, light, 0, 0, size, lineSize, 0.5F * alpha, fillColor);
						if (!chunks.contains(new ChunkPos(chunk.x() + 1, chunk.z()))) DrawUtil.fill(matrices, submitter, RenderTypes.textBackgroundSeeThrough(), z, light, size - lineSize, 0, size, size, 0.5F * alpha, fillColor);
						if (!chunks.contains(new ChunkPos(chunk.x(), chunk.z() + 1))) DrawUtil.fill(matrices, submitter, RenderTypes.textBackgroundSeeThrough(), z, light, 0, size - lineSize, size, size, 0.5F * alpha, fillColor);
					}
				}
				matrices.popPose();
			}
			return;
		}

		double markerX = worldXToScreenX(pos.getX()) - bookX();
		double markerY = worldZToScreenY(pos.getZ()) - bookY();

		if (pinned) {
			markerX = Mth.clamp(markerX, MAP_BORDER_WIDTH, mapWidth() + MAP_BORDER_WIDTH);
			markerY = Mth.clamp(markerY, MAP_BORDER_HEIGHT, mapHeight() + MAP_BORDER_HEIGHT);
		}


		texture.draw(matrices, submitter, markerX, markerY, z, markerScale, tileChunks(), accent, tint, alphaGetter.apply(markerX, markerY), light);
	}

	default void renderMarker(GuiGraphicsExtractor context, Landmark landmark, MarkerTexture texture, BiFunction<Double, Double, Float> alphaGetter, boolean pinned, boolean hovering, float markerScale) {
		BlockPos pos = landmark.get(LandmarkComponentTypes.POS);
		Integer color = landmark.get(LandmarkComponentTypes.COLOR);
		float[] accent = color == null ? null : ColorUtil.componentsFromRgb(color);
		float tint = hovering ? 0.8f : 1.0f;

		if (pos == null) {
			Set<ChunkPos> chunks = RegionPos.regionsToChunks(landmark.getOrDefault(LandmarkComponentTypes.CHUNKS, new HashMap<>()));
			for (ChunkPos chunk : chunks) {
				double markerX = worldXToScreenX(chunk.getMinBlockX()) - bookX();
				double markerY = worldZToScreenY(chunk.getMinBlockZ()) - bookY();
				float effectiveScale = (float) (mapScale() / guiScale());
				int size = Math.max(1, Math.round((tilePixels() / (float) tileChunks()) * effectiveScale));
				int lineSize = Math.max(1, Math.round((tilePixels() / 16.0F) * effectiveScale));
				float[] fillColor = accent == null ? ColorUtil.componentsFromRgb(0xFFFFFF) : new float[] { tint * accent[0], tint * accent[1], tint * accent[2] };
				float alpha = alphaGetter.apply(markerX, markerY);
				int x = bookX() + Math.round((float) markerX);
				int y = bookY() + Math.round((float) markerY);
				fillGui(context, x, y, x + size, y + size, 0.25F * alpha, fillColor);
				if (!chunks.contains(new ChunkPos(chunk.x() - 1, chunk.z()))) fillGui(context, x, y, x + lineSize, y + size, 0.5F * alpha, fillColor);
				if (!chunks.contains(new ChunkPos(chunk.x(), chunk.z() - 1))) fillGui(context, x, y, x + size, y + lineSize, 0.5F * alpha, fillColor);
				if (!chunks.contains(new ChunkPos(chunk.x() + 1, chunk.z()))) fillGui(context, x + size - lineSize, y, x + size, y + size, 0.5F * alpha, fillColor);
				if (!chunks.contains(new ChunkPos(chunk.x(), chunk.z() + 1))) fillGui(context, x, y + size - lineSize, x + size, y + size, 0.5F * alpha, fillColor);
			}
			return;
		}

		double markerX = worldXToScreenX(pos.getX()) - bookX();
		double markerY = worldZToScreenY(pos.getZ()) - bookY();

		if (pinned) {
			markerX = Mth.clamp(markerX, MAP_BORDER_WIDTH, mapWidth() + MAP_BORDER_WIDTH);
			markerY = Mth.clamp(markerY, MAP_BORDER_HEIGHT, mapHeight() + MAP_BORDER_HEIGHT);
		}

		texture.draw(context, bookX() + markerX, bookY() + markerY, markerScale, tileChunks(), accent, tint, alphaGetter.apply(markerX, markerY));
	}

	default void renderPlayer(PoseStack matrices, SubmitNodeCollector submitter, float z, int light, PlayerSummary player, float iconScale, float alpha, boolean hovering, boolean self) {
		double dimX = player.pos().x();
		double dimZ = player.pos().z();

		boolean inDim = dim().equals(player.dimension());
		if (!inDim) {
			Map<ResourceKey<Level>, Integer> scales = AntiqueAtlas.CONFIG.dimensions.getScales(Minecraft.getInstance().getConnection());
			int newScale = scales.getOrDefault(dim(), 0);
			int oldScale = scales.getOrDefault(player.dimension(), 0);
			if (newScale * oldScale == 0) return; // no ratio!
			double mult = newScale / (double) oldScale;
			dimX = mult * dimX;
			dimZ = mult * dimZ;
		}

		double playerOffsetX = worldXToScreenX(dimX) - bookX();
		double playerOffsetY = worldZToScreenY(dimZ) - bookY();

		playerOffsetX = Mth.clamp(playerOffsetX, MAP_BORDER_WIDTH, mapWidth() + MAP_BORDER_WIDTH);
		playerOffsetY = Mth.clamp(playerOffsetY, MAP_BORDER_HEIGHT, mapHeight() + MAP_BORDER_HEIGHT);

		// Draw the icon:
		float tint = (player.online() ? 1 : 0.5f) * (hovering ? 0.9f : 1);
		float greenTint = self ? 1 : 0.7f;
		float redTint = inDim ? 1 : 0.7f;
		int argb = ARGB.color((int) (alpha * 255.0), (int) (tint * redTint * 255), (int) (tint * greenTint * 255), (int) (tint * 255));
		float playerRotation = ((float) Math.round(player.yaw() / 360f * PLAYER_ROTATION_STEPS) / PLAYER_ROTATION_STEPS) * 360f;

		DrawUtil.drawCenteredWithRotation(matrices, submitter, PLAYER, playerOffsetX, playerOffsetY, z, iconScale, PLAYER_ICON_WIDTH, PLAYER_ICON_HEIGHT, playerRotation, light, argb);
	}

	default void renderPlayer(GuiGraphicsExtractor context, PlayerSummary player, float iconScale, float alpha, boolean hovering, boolean self) {
		double dimX = player.pos().x();
		double dimZ = player.pos().z();

		boolean inDim = dim().equals(player.dimension());
		if (!inDim) {
			Map<ResourceKey<Level>, Integer> scales = AntiqueAtlas.CONFIG.dimensions.getScales(Minecraft.getInstance().getConnection());
			int newScale = scales.getOrDefault(dim(), 0);
			int oldScale = scales.getOrDefault(player.dimension(), 0);
			if (newScale * oldScale == 0) return;
			double mult = newScale / (double) oldScale;
			dimX = mult * dimX;
			dimZ = mult * dimZ;
		}

		double playerOffsetX = Mth.clamp(worldXToScreenX(dimX) - bookX(), MAP_BORDER_WIDTH, mapWidth() + MAP_BORDER_WIDTH);
		double playerOffsetY = Mth.clamp(worldZToScreenY(dimZ) - bookY(), MAP_BORDER_HEIGHT, mapHeight() + MAP_BORDER_HEIGHT);

		float tint = (player.online() ? 1 : 0.5f) * (hovering ? 0.9f : 1);
		float greenTint = self ? 1 : 0.7f;
		float redTint = inDim ? 1 : 0.7f;
		int argb = ARGB.color((int) (alpha * 255.0), (int) (tint * redTint * 255), (int) (tint * greenTint * 255), (int) (tint * 255));
		float playerRotation = ((float) Math.round(player.yaw() / 360f * PLAYER_ROTATION_STEPS) / PLAYER_ROTATION_STEPS) * 360f;

		var pose = context.pose();
		pose.pushMatrix();
		pose.translate((float) (bookX() + playerOffsetX), (float) (bookY() + playerOffsetY));
		pose.rotate((float) Math.toRadians(180 + playerRotation));
		pose.scale(iconScale);
		context.blit(RenderPipelines.GUI_TEXTURED, PLAYER, -PLAYER_ICON_WIDTH / 2, -PLAYER_ICON_HEIGHT / 2, 0, 0, PLAYER_ICON_WIDTH, PLAYER_ICON_HEIGHT, PLAYER_ICON_WIDTH, PLAYER_ICON_HEIGHT, argb);
		pose.popMatrix();
	}

	default void renderTiles(PoseStack matrices, SubmitNodeCollector submitter, int light) {
		int mapStartChunkX = MathUtil.roundToBase(screenXToWorldX(bookX()) >> 4, tileChunks()) - 2 * tileChunks();
		int mapStartChunkZ = MathUtil.roundToBase(screenYToWorldZ(bookY()) >> 4, tileChunks()) - 2 * tileChunks();
		int mapEndChunkX = MathUtil.roundToBase(screenXToWorldX(bookX() + bookWidth()) >> 4, tileChunks()) + 2 * tileChunks();
		int mapEndChunkZ = MathUtil.roundToBase(screenYToWorldZ(bookY() + bookHeight()) >> 4, tileChunks()) + 2 * tileChunks();
		double mapStartScreenX = worldXToScreenX(mapStartChunkX << 4);
		double mapStartScreenY = worldZToScreenY(mapStartChunkZ << 4);
		TileRenderIterator tiles = new TileRenderIterator(worldAtlasData());
		tiles.setScope(new Rect(mapStartChunkX, mapStartChunkZ, mapEndChunkX, mapEndChunkZ));
		tiles.setStep(tileChunks());
		int mapX = bookX() + MAP_BORDER_WIDTH;
		int mapY = bookY() + MAP_BORDER_HEIGHT;
		float effectiveScale = (float) (mapScale() / guiScale());
		matrices.pushPose();
		matrices.translate(Math.round(mapStartScreenX), Math.round(mapStartScreenY), 0);
		matrices.scale(effectiveScale, effectiveScale, 1.0F);

		Map<TileTexture, Collection<SubTile>> tileTextures = new Reference2ObjectArrayMap<>();
		for (SubTileQuartet subTiles : tiles) {
			for (SubTile subtile : subTiles) {
				if (subtile == null || subtile.texture == null) continue;
				tileTextures.computeIfAbsent(subtile.texture, k -> new ArrayList<>()).add(subtile.copy());
			}
		}
		int subTilePixels = tilePixels() / 2;
		tileTextures.forEach((texture, subtiles) -> {
			try (DrawBatcher batcher = new DrawBatcher(matrices, submitter, texture.id(), 32, 48, light, true)) {
				for (SubTile subtile : subtiles) {
					int drawX = subtile.x * subTilePixels;
					int drawY = subtile.y * subTilePixels;
					// a non-scope bounds check allows subtile-level accuracy, and keeps border tiling accurate.
					if (drawX * effectiveScale > mapX + mapWidth() - mapStartScreenX || drawY * effectiveScale > mapY + mapHeight() - mapStartScreenY || (drawX + subTilePixels) * effectiveScale < mapX - mapStartScreenX || (drawY + subTilePixels) * effectiveScale < mapY - mapStartScreenY) continue;
					batcher.add(drawX, drawY, 0, subTilePixels, subTilePixels, subtile.getTextureU() * 8, subtile.getTextureV() * 8, 8, 8, 0xFFFFFFFF);
				}
			}
		});

		matrices.popPose();
	}

	default void renderTiles(GuiGraphicsExtractor context, int argb) {
		int mapStartChunkX = MathUtil.roundToBase(screenXToWorldX(bookX()) >> 4, tileChunks()) - 2 * tileChunks();
		int mapStartChunkZ = MathUtil.roundToBase(screenYToWorldZ(bookY()) >> 4, tileChunks()) - 2 * tileChunks();
		int mapEndChunkX = MathUtil.roundToBase(screenXToWorldX(bookX() + bookWidth()) >> 4, tileChunks()) + 2 * tileChunks();
		int mapEndChunkZ = MathUtil.roundToBase(screenYToWorldZ(bookY() + bookHeight()) >> 4, tileChunks()) + 2 * tileChunks();
		double mapStartScreenX = worldXToScreenX(mapStartChunkX << 4);
		double mapStartScreenY = worldZToScreenY(mapStartChunkZ << 4);
		TileRenderIterator tiles = new TileRenderIterator(worldAtlasData());
		tiles.setScope(new Rect(mapStartChunkX, mapStartChunkZ, mapEndChunkX, mapEndChunkZ));
		tiles.setStep(tileChunks());
		int mapX = bookX() + MAP_BORDER_WIDTH;
		int mapY = bookY() + MAP_BORDER_HEIGHT;
		float effectiveScale = (float) (mapScale() / guiScale());

		Map<TileTexture, Collection<SubTile>> tileTextures = new Reference2ObjectArrayMap<>();
		for (SubTileQuartet subTiles : tiles) {
			for (SubTile subtile : subTiles) {
				if (subtile == null || subtile.texture == null) continue;
				tileTextures.computeIfAbsent(subtile.texture, k -> new ArrayList<>()).add(subtile.copy());
			}
		}
		int subTilePixels = tilePixels() / 2;
		tileTextures.forEach((texture, subtiles) -> {
			for (SubTile subtile : subtiles) {
				int drawX = subtile.x * subTilePixels;
				int drawY = subtile.y * subTilePixels;
				if (drawX * effectiveScale > mapX + mapWidth() - mapStartScreenX || drawY * effectiveScale > mapY + mapHeight() - mapStartScreenY || (drawX + subTilePixels) * effectiveScale < mapX - mapStartScreenX || (drawY + subTilePixels) * effectiveScale < mapY - mapStartScreenY) continue;
				int x = (int) Math.round(mapStartScreenX + drawX * effectiveScale);
				int y = (int) Math.round(mapStartScreenY + drawY * effectiveScale);
				int size = Math.max(1, Math.round(subTilePixels * effectiveScale));
				context.blit(RenderPipelines.GUI_TEXTURED, texture.id(), x, y, subtile.getTextureU() * 8, subtile.getTextureV() * 8, size, size, 8, 8, 32, 48, argb);
			}
		});
	}

	default void fillGui(GuiGraphicsExtractor context, int x1, int y1, int x2, int y2, float alpha, float[] color) {
		context.fill(x1, y1, x2, y2, ARGB.color((int) (alpha * 255.0F), (int) (color[0] * 255.0F), (int) (color[1] * 255.0F), (int) (color[2] * 255.0F)));
	}
}
