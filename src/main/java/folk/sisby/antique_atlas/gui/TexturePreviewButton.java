package folk.sisby.antique_atlas.gui;

import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.gui.core.ToggleButtonComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;


public class TexturePreviewButton<T> extends ToggleButtonComponent {
	public static final Identifier FRAME_SELECTED = AntiqueAtlas.id("textures/gui/frame_selected.png");
	public static final Identifier FRAME_UNSELECTED = AntiqueAtlas.id("textures/gui/frame.png");
	public static final int FRAME_SIZE = 34;

	protected final T value;
	protected final Identifier texture;
	protected final int textureWidth;
	protected final int textureHeight;
	protected final int v;
	protected float[] tint;

	public TexturePreviewButton(T value, Identifier texture, int textureWidth, int textureHeight, int v, float[] tint) {
		super(false);
		this.value = value;
		this.texture = texture;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
		this.v = v;
		this.tint = tint;
		setSize(FRAME_SIZE, FRAME_SIZE);
	}

	public T getValue() {
		return value;
	}

	public void reTint(float[] tint) {
		if (this.tint != null) this.tint = tint;
	}

	protected void drawTexture(GuiGraphicsExtractor context, int x, int y) {
		int color = tint == null ? 0xFFFFFFFF : ARGB.colorFromFloat(1.0F, tint[0], tint[1], tint[2]);
		context.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, v, textureWidth, textureHeight, textureWidth, textureHeight + v, color);
	}

	@Override
	public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
		Identifier frameTexture = isSelected() ? FRAME_SELECTED : FRAME_UNSELECTED;
		context.blit(RenderPipelines.GUI_TEXTURED, frameTexture, getGuiX() + 1, getGuiY() + 1, 0, 0, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE);

		int centerX = getGuiX() + (FRAME_SIZE - textureWidth) / 2;
		int centerY = getGuiY() + (FRAME_SIZE - textureHeight) / 2;
		drawTexture(context, centerX, centerY);

		super.render(context, mouseX, mouseY, partialTick);
	}
}
