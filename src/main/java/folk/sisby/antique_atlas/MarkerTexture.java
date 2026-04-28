package folk.sisby.antique_atlas;

import com.mojang.blaze3d.vertex.PoseStack;
import folk.sisby.antique_atlas.util.DrawBatcher;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Vector2d;

public record MarkerTexture(Identifier id, Identifier accentId, Identifier item, int offsetX, int offsetY, int textureWidth, int textureHeight, int mipLevels, int nearClip, int farClip) {
	public static Identifier idToTexture(Identifier id) {
		return id.withPrefix("textures/atlas/marker/").withSuffix(".png");
	}

	public static MarkerTexture ofId(Identifier id, Identifier item, int offsetX, int offsetY, int width, int height, int mipLevels, int nearClip, int farClip, boolean accent) {
		return new MarkerTexture(idToTexture(id), accent ? idToTexture(id.withSuffix("_accent")) : null, item, offsetX, offsetY, width, height, mipLevels, nearClip, farClip);
	}

	public static MarkerTexture centered(Identifier id, Identifier item, int width, int height, int mipLevels, int nearClip, int farClip, boolean accent) {
		return ofId(id, item, -width / 2, -height / 2, width, height, mipLevels, nearClip, farClip, accent);
	}

	public static final MarkerTexture DEFAULT = centered(AntiqueAtlas.id("custom/point"), Identifier.fromNamespaceAndPath("minecraft", "emerald"), 32, 32, 0, 1, Integer.MAX_VALUE, true);

	public Identifier keyId() {
		return Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath().substring("textures/atlas/marker/".length(), id.getPath().length() - 4));
	}

	public String displayId() {
		return id.getNamespace().equals(AntiqueAtlas.ID) ? keyId().getPath() : keyId().toString();
	}

	public int fullTextureWidth() {
		int width = textureWidth;
		for (int i = 0; i < mipLevels; i++) {
			width += textureWidth >> (i + 1);
		}
		return width;
	}

	public int getU(int mipLevel) {
		int currentMipLevel = mipLevel - 1;
		int u = 0;
		while (currentMipLevel >= 0) {
			u += textureWidth / (1 << currentMipLevel);
			currentMipLevel--;
		}
		return u;
	}

	public Vector2d getCenter(int tileChunks) {
		int mipLevel = Mth.clamp(Mth.ceillog2(tileChunks), 0, mipLevels);
		return new Vector2d(((double) offsetX + (double) textureWidth / 2.0) / (double) (1 << mipLevel), ((double) offsetY + (double) textureHeight / 2.0) / (double) (1 << mipLevel));
	}

	public double getSquaredSize(int tileChunks) {
		int mipLevel = Mth.clamp(Mth.ceillog2(tileChunks), 0, mipLevels);
		return textureWidth * textureHeight / (double) (1 << mipLevel);
	}

	public void drawIcon(GuiGraphicsExtractor context, int x, int y, float[] accent) {
		context.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0, 0, textureWidth, textureHeight, fullTextureWidth(), textureHeight);
		if (accentId != null && accent != null) {
			context.blit(RenderPipelines.GUI_TEXTURED, accentId, x, y, 0, 0, textureWidth, textureHeight, fullTextureWidth(), textureHeight, ARGB.colorFromFloat(1F, accent[0], accent[1], accent[2]));
		}
	}

	public void draw(GuiGraphicsExtractor context, double markerX, double markerY, float markerScale, int tileChunks, float[] accent, float tint, float alpha) {
		if (alpha == 0) return;
		int mainArgb = ARGB.color((int) (alpha * 255), (int) (tint * 255), (int) (tint * 255), (int) (tint * 255));
		int accentArgb = accent != null ? ARGB.color((int) (alpha * 255), (int) (tint * accent[0] * 255), (int) (tint * accent[1] * 255), (int) (tint * accent[2] * 255)) : 0;
		if (tileChunks > 1 && mipLevels > 0) {
			int mipLevel = Mth.clamp(Mth.ceillog2(tileChunks), 0, mipLevels);
			drawGuiRegion(context, id, markerX, markerY, markerScale, offsetX / (1 << mipLevel), offsetY / (1 << mipLevel), getU(mipLevel), 0, textureWidth / (1 << mipLevel), textureHeight / (1 << mipLevel), mainArgb);
			if (accentId != null && accent != null) {
				drawGuiRegion(context, accentId, markerX, markerY, markerScale, offsetX / (1 << mipLevel), offsetY / (1 << mipLevel), getU(mipLevel), 0, textureWidth / (1 << mipLevel), textureHeight / (1 << mipLevel), accentArgb);
			}
		} else {
			drawGuiRegion(context, id, markerX, markerY, markerScale, offsetX, offsetY, 0, 0, textureWidth, textureHeight, mainArgb);
			if (accentId != null && accent != null) {
				drawGuiRegion(context, accentId, markerX, markerY, markerScale, offsetX, offsetY, 0, 0, textureWidth, textureHeight, accentArgb);
			}
		}
	}

	protected void drawGuiRegion(GuiGraphicsExtractor context, Identifier texture, double markerX, double markerY, float markerScale, int offsetX, int offsetY, int u, int v, int regionWidth, int regionHeight, int argb) {
		int x = (int) Math.round(markerX + markerScale * offsetX);
		int y = (int) Math.round(markerY + markerScale * offsetY);
		int width = Math.max(1, Math.round(markerScale * regionWidth));
		int height = Math.max(1, Math.round(markerScale * regionHeight));
		context.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, regionWidth, regionHeight, fullTextureWidth(), textureHeight, argb);
	}

	public void draw(PoseStack matrices, SubmitNodeCollector submitter, double markerX, double markerY, float z, float markerScale, int tileChunks, float[] accent, float tint, float alpha, int light) {
		draw(matrices, (OrderedSubmitNodeCollector) submitter, markerX, markerY, z, markerScale, tileChunks, accent, tint, alpha, light);
	}

	public void draw(PoseStack matrices, OrderedSubmitNodeCollector submitter, double markerX, double markerY, float z, float markerScale, int tileChunks, float[] accent, float tint, float alpha, int light) {
		if (alpha == 0) return;
		matrices.pushPose();
		matrices.translate(markerX, markerY, 0.0);
		matrices.scale(markerScale, markerScale, 1.0F);
		int mainArgb = ARGB.color((int) (alpha * 255), (int) (tint * 255), (int) (tint * 255), (int) (tint * 255));
		int accentArgb = accent != null ? ARGB.color((int) (alpha * 255), (int) (tint * accent[0] * 255), (int) (tint * accent[1] * 255), (int) (tint * accent[2] * 255)) : 0;
		if (tileChunks > 1 && mipLevels > 0) {
			int mipLevel = Mth.clamp(Mth.ceillog2(tileChunks), 0, mipLevels);
			DrawBatcher.drawSingle(matrices, submitter, id, fullTextureWidth(), textureHeight, light, offsetX / (1 << mipLevel), offsetY / (1 << mipLevel), z, textureWidth / (1 << mipLevel), textureHeight / (1 << mipLevel), getU(mipLevel), 0, textureWidth / (1 << mipLevel), textureHeight / (1 << mipLevel), mainArgb, false);
			if (accentId != null && accent != null) {
				DrawBatcher.drawSingle(matrices, submitter, accentId, fullTextureWidth(), textureHeight, light, offsetX / (1 << mipLevel), offsetY / (1 << mipLevel), z, textureWidth / (1 << mipLevel), textureHeight / (1 << mipLevel), getU(mipLevel), 0, textureWidth / (1 << mipLevel), textureHeight / (1 << mipLevel), accentArgb, false);
			}
		} else {
			DrawBatcher.drawSingle(matrices, submitter, id, fullTextureWidth(), textureHeight, light, offsetX, offsetY, z, textureWidth, textureHeight, 0, 0, textureWidth, textureHeight, mainArgb, false);
			if (accentId != null && accent != null) {
				DrawBatcher.drawSingle(matrices, submitter, accentId, fullTextureWidth(), textureHeight, light, offsetX, offsetY, z, textureWidth, textureHeight, 0, 0, textureWidth, textureHeight, accentArgb, false);
			}
		}
		matrices.popPose();
	}
}
