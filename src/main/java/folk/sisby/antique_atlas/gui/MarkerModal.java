package folk.sisby.antique_atlas.gui;

import folk.sisby.antique_atlas.MarkerTexture;
import folk.sisby.antique_atlas.WorldAtlasData;
import folk.sisby.antique_atlas.gui.core.Component;
import folk.sisby.antique_atlas.gui.core.ScrollBoxComponent;
import folk.sisby.antique_atlas.gui.core.ToggleButtonRadioGroup;
import folk.sisby.antique_atlas.reloader.MarkerTextures;
import folk.sisby.antique_atlas.util.ColorUtil;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.WorldLandmarks;
import folk.sisby.surveyor.landmark.component.LandmarkComponentTypes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

/**
 * This GUI is used select marker icon and enter a label.
 * When the user clicks on the confirmation button, the call to MarkerAPI is made.
 *
 * @author Hunternif
 */
public class MarkerModal extends Component {
	protected WorldSummary summary;
	protected RegistryAccess manager;
	protected Landmark baseLandmark = null;

	protected MarkerTexture selectedTexture = MarkerTexture.DEFAULT;
	protected DyeColor selectedColor = DyeColor.WHITE;

	public static final int BUTTON_WIDTH = 80;
	public static final int BUTTON_SPACING = 8;

	public static final int TYPE_SPACING = 1;

	protected Button btnDone;
	protected Button btnCancel;
	protected EditBox textField;
	protected ScrollBoxComponent textureScrollBox;
	protected ToggleButtonRadioGroup<TexturePreviewButton<MarkerTexture>> textureRadioGroup;
	protected ScrollBoxComponent colorScrollBox;
	protected ToggleButtonRadioGroup<TexturePreviewButton<DyeColor>> colorRadioGroup;
	protected Map<MarkerTexture, TexturePreviewButton<MarkerTexture>> textureButtons = new LinkedHashMap<>();
	protected Map<DyeColor, TexturePreviewButton<DyeColor>> colorButtons = new LinkedHashMap<>();

	protected final List<IMarkerTypeSelectListener> markerListeners = new ArrayList<>();

	public MarkerModal() {
	}

	void setMarkerData(WorldSummary summary, RegistryAccess manager, Landmark baseLandmark) {
		this.summary = summary;
		this.manager = manager;
		this.baseLandmark = baseLandmark;
		this.selectedColor = Arrays.stream(DyeColor.values()).filter(d -> baseLandmark.contains(LandmarkComponentTypes.COLOR) && d.getTextureDiffuseColor() == baseLandmark.get(LandmarkComponentTypes.COLOR)).findAny().orElse(DyeColor.WHITE);
		this.selectedTexture = MarkerTextures.getInstance().fromLandmark(baseLandmark);
		if (!selectedTexture.keyId().getPath().startsWith("custom/")) selectedTexture = textureButtons.keySet().stream().findFirst().orElse(MarkerTexture.DEFAULT);
		if (colorRadioGroup != null) updateSelected();
	}

	void addMarkerListener(IMarkerTypeSelectListener listener) {
		markerListeners.add(listener);
	}

	protected void updateSelected() {
		colorRadioGroup.setSelectedButton(colorButtons.get(selectedColor));
		textureRadioGroup.setSelectedButton(textureButtons.get(selectedTexture));
	}

	@Override
	public void init() { // set up in here because it scales to parent size
		removeAllChildren();
		super.init();

		addRenderableWidget(btnDone = Button.builder(net.minecraft.network.chat.Component.translatable("gui.done"), (button) -> {
			MutableComponent label = net.minecraft.network.chat.Component.literal(textField.getValue());
			WorldLandmarks landmarks = summary.landmarks();
			if (landmarks != null) {
				landmarks.remove(baseLandmark.owner(), baseLandmark.id());
				landmarks.put(WorldAtlasData.copyLandmarkWith(
					baseLandmark,
					selectedTexture.keyId().withSuffix("/" + selectedColor.getName() + "/" + baseLandmark.get(LandmarkComponentTypes.POS).getX() + "/" + baseLandmark.get(LandmarkComponentTypes.POS).getZ()),
					copy -> {
					Item item = manager.lookupOrThrow(Registries.ITEM).getValue(selectedTexture.item());
					if (item != null && !item.getDefaultInstance().isEmpty()) copy.set(LandmarkComponentTypes.STACK, item.getDefaultInstance().copy());
					copy.set(LandmarkComponentTypes.COLOR, selectedColor.getTextureDiffuseColor());
					copy.set(LandmarkComponentTypes.NAME, label);
				}));
			}
			((AtlasScreen) getParent()).updateBookmarkerList();
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 1F));
			closeChild();
		}).bounds(this.width / 2 - BUTTON_WIDTH - BUTTON_SPACING / 2, this.height / 2 + 70, BUTTON_WIDTH, 20).build());
		addRenderableWidget(btnCancel = Button.builder(net.minecraft.network.chat.Component.translatable("gui.cancel"), (button) -> closeChild())
			.bounds(this.width / 2 + BUTTON_SPACING / 2, this.height / 2 + 70, BUTTON_WIDTH, 20).build());
		textField = new EditBox(Minecraft.getInstance().font, (this.width - 200) / 2, this.height / 2 - 65, 200, 20, net.minecraft.network.chat.Component.translatable("gui.antique_atlas.marker.label"));
		textField.setEditable(true);
		textField.setCanLoseFocus(true);
		textField.setFocused(true);
		textField.setHint(net.minecraft.network.chat.Component.translatable("gui.antique_atlas.marker.label"));
		textField.setValue(baseLandmark.getOrDefault(LandmarkComponentTypes.NAME, net.minecraft.network.chat.Component.empty()).getString());

		textureScrollBox = new ScrollBoxComponent(false, (TexturePreviewButton.FRAME_SIZE + TYPE_SPACING));
		this.addChild(textureScrollBox);

		int typeCount = (int) MarkerTextures.getInstance().asMap().values().stream().filter(t -> t.keyId().getPath().startsWith("custom/")).count();
		int typesOnScreen = Math.min(typeCount, 7);
		int typeScrollWidth = typesOnScreen * (TexturePreviewButton.FRAME_SIZE + TYPE_SPACING) - TYPE_SPACING;
		textureScrollBox.getViewport().setSize(typeScrollWidth, TexturePreviewButton.FRAME_SIZE + TYPE_SPACING);
		textureScrollBox.setGuiCoords((this.width - typeScrollWidth) / 2, this.height / 2 - 35);

		textureRadioGroup = new ToggleButtonRadioGroup<>();
		textureRadioGroup.addListener(button -> {
			selectedTexture = button.getValue();
			for (IMarkerTypeSelectListener listener : markerListeners) {
				listener.onSelectMarkerType(button.getValue());
			}
		});
		int contentX = 0;
		for (MarkerTexture texture : MarkerTextures.getInstance().asMap().values()) {
			if (!texture.keyId().getPath().startsWith("custom/")) continue;
			if (selectedTexture == MarkerTexture.DEFAULT) selectedTexture = texture;
			TexturePreviewButton<MarkerTexture> markerGui = new MarkerPreviewButton(texture, ColorUtil.componentsFromRgb(selectedColor.getFireworkColor()));
			textureButtons.put(texture, markerGui);
			textureRadioGroup.addButton(markerGui);
			textureScrollBox.getViewport().addContent(markerGui).setRelativeX(contentX);
			contentX += TexturePreviewButton.FRAME_SIZE + TYPE_SPACING;
		}

		// Color

		colorScrollBox = new ScrollBoxComponent(false, (TexturePreviewButton.FRAME_SIZE + TYPE_SPACING));
		this.addChild(colorScrollBox);

		int colorsOnScreen = Math.min(DyeColor.values().length, 7);
		int colorScrollWidth = colorsOnScreen * (TexturePreviewButton.FRAME_SIZE + TYPE_SPACING) - TYPE_SPACING;
		colorScrollBox.getViewport().setSize(colorScrollWidth, TexturePreviewButton.FRAME_SIZE + TYPE_SPACING);
		colorScrollBox.setGuiCoords((this.width - colorScrollWidth) / 2, this.height / 2 + 10);

		colorRadioGroup = new ToggleButtonRadioGroup<>();
		colorRadioGroup.addListener(button -> {
			selectedColor = button.getValue();
			for (TexturePreviewButton<MarkerTexture> preview : textureRadioGroup) {
				preview.reTint(ColorUtil.componentsFromRgb(selectedColor.getFireworkColor()));
			}
		});
		int colorContentX = 0;
		for (DyeColor color : DyeColor.values()) {
			TexturePreviewButton<DyeColor> colorGui = new TexturePreviewButton<>(color, BookmarkButton.TEXTURE_LEFT, BookmarkButton.WIDTH, BookmarkButton.HEIGHT, BookmarkButton.HEIGHT, ColorUtil.componentsFromRgb(color.getFireworkColor()));
			colorButtons.put(color, colorGui);
			colorRadioGroup.addButton(colorGui);
			colorScrollBox.getViewport().addContent(colorGui).setRelativeX(colorContentX);
			colorContentX += TexturePreviewButton.FRAME_SIZE + TYPE_SPACING;
		}

		updateSelected();
	}

	@Override
	public void closeChild() {
		super.closeChild();
		if (textureScrollBox != null) {
			textureScrollBox.closeChild();
		}
		if (colorScrollBox != null) {
			colorScrollBox.closeChild();
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0));
		return btnDone.mouseClicked(event, false) || btnCancel.mouseClicked(event, false) || textField.mouseClicked(event, false) || super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int aa, int bb, int cc) {
		return super.keyPressed(aa, bb, cc) || textField.keyPressed(new KeyEvent(aa, bb, cc));
	}

	@Override
	public boolean charTyped(char aa, int bb) {
		return super.charTyped(aa, bb) || textField.charTyped(new CharacterEvent(aa));
	}

	@Override
	public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
		context.fill(0, 0, width, height, 0x99000000);
		drawCentered(context, net.minecraft.network.chat.Component.translatable("gui.antique_atlas.marker.label"), this.height / 2 - 80, 0xDDDDDD, true);
		btnCancel.extractRenderState(context, mouseX, mouseY, partialTick);
		btnDone.extractRenderState(context, mouseX, mouseY, partialTick);
		textField.extractRenderState(context, mouseX, mouseY, partialTick);
		// Darker background for marker type selector
		context.fillGradient(textureScrollBox.getGuiX() + 1, textureScrollBox.getGuiY() + 1,
			textureScrollBox.getGuiX() + textureScrollBox.getWidth(),
			textureScrollBox.getGuiY() + textureScrollBox.getHeight(),
			0x88101010, 0x99101010);
		context.fillGradient(colorScrollBox.getGuiX() + 1, colorScrollBox.getGuiY() + 1,
			colorScrollBox.getGuiX() + colorScrollBox.getWidth(),
			colorScrollBox.getGuiY() + colorScrollBox.getHeight(),
			0x88101010, 0x99101010);
		super.render(context, mouseX, mouseY, partialTick);
	}

	public interface IMarkerTypeSelectListener {
		void onSelectMarkerType(MarkerTexture texture);
	}
}
