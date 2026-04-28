package folk.sisby.antique_atlas.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

public class DrawUtil {
	public static void drawCenteredWithRotation(PoseStack matrices, SubmitNodeCollector submitter, Identifier texture, double x, double y, float z, float scale, int textureWidth, int textureHeight, float rotation, int light, int argb) {
		drawCenteredWithRotation(matrices, (OrderedSubmitNodeCollector) submitter, texture, x, y, z, scale, textureWidth, textureHeight, rotation, light, argb);
	}

	public static void drawCenteredWithRotation(PoseStack matrices, OrderedSubmitNodeCollector submitter, Identifier texture, double x, double y, float z, float scale, int textureWidth, int textureHeight, float rotation, int light, int argb) {
		matrices.pushPose();
		matrices.translate(x, y, 0.0);
		matrices.scale(scale, scale, 1.0F);
		matrices.mulPose(Axis.ZP.rotationDegrees(180 + rotation));
		matrices.translate(-textureWidth / 2f, -textureHeight / 2f, 0f);
		DrawBatcher.drawSingle(matrices, submitter, texture, textureWidth, textureHeight, light, 0, 0, z, textureWidth, textureHeight, 0, 0, textureWidth, textureHeight, argb, false);
		matrices.popPose();
	}

	public static void fill(PoseStack matrices, SubmitNodeCollector submitter, RenderType layer, float z, int light, int x1, int y1, int x2, int y2, float alpha, float[] color) {
		fill(matrices, (OrderedSubmitNodeCollector) submitter, layer, z, light, x1, y1, x2, y2, alpha, color);
	}

	public static void fill(PoseStack matrices, OrderedSubmitNodeCollector submitter, RenderType layer, float z, int light, int x1, int y1, int x2, int y2, float alpha, float[] color) {
		BufferBuilder bufferBuilder = null;
		VertexConsumer vertexConsumer;
		if (submitter == null) {
			bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_LIGHTMAP);
			vertexConsumer = bufferBuilder;
		} else {
			PoseStack submittedMatrices = new PoseStack();
			submittedMatrices.last().set(matrices.last());
			submitter.submitCustomGeometry(submittedMatrices, layer, (pose, consumer) -> addFillVertices(consumer, pose.pose(), z, light, x1, y1, x2, y2, alpha, color));
			return;
		}

		addFillVertices(vertexConsumer, matrices.last().pose(), z, light, x1, y1, x2, y2, alpha, color);
		if (bufferBuilder != null) {
			MeshData meshData = bufferBuilder.buildOrThrow();
			layer.draw(meshData);
		}
	}

	protected static void addFillVertices(VertexConsumer vertexConsumer, Matrix4f matrix4f, float z, int light, int x1, int y1, int x2, int y2, float alpha, float[] color) {
		vertexConsumer.addVertex(matrix4f, x1, y1, z).setColor(color[0], color[1], color[2], alpha).setLight(light);
		vertexConsumer.addVertex(matrix4f, x1, y2, z).setColor(color[0], color[1], color[2], alpha).setLight(light);
		vertexConsumer.addVertex(matrix4f, x2, y2, z).setColor(color[0], color[1], color[2], alpha).setLight(light);
		vertexConsumer.addVertex(matrix4f, x2, y1, z).setColor(color[0], color[1], color[2], alpha).setLight(light);
	}
}
