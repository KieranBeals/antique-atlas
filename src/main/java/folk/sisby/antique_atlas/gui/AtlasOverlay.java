package folk.sisby.antique_atlas.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import folk.sisby.surveyor.PlayerSummary;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.SubmitNodeCollector;

public interface AtlasOverlay {
	default void onScreenInit(AtlasScreen screen) {
	}

	default void onScreenRender(AtlasScreenRenderContext context) {
	}

	default void onRender(AtlasRenderContext context) {
	}

	record AtlasScreenRenderContext(AtlasScreen screen, GuiGraphicsExtractor context, int mouseX, int mouseY, float markerScale, Map<UUID, PlayerSummary> friends) {
	}

	record AtlasRenderContext(AtlasRenderer renderer, PoseStack matrices, SubmitNodeCollector submitter, @Nullable Integer mouseX, @Nullable Integer mouseY, int light, float markerScale, Map<UUID, PlayerSummary> friends) {
	}
}
