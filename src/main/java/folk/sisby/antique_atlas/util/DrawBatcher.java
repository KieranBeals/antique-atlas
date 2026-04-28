package folk.sisby.antique_atlas.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Method;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class DrawBatcher implements AutoCloseable {

	protected final Matrix4f matrix4f;
	protected final BufferBuilder bufferBuilder;
	protected final VertexConsumer vertexConsumer;
	protected final SubmitNodeCollector submitter;
	protected final PoseStack submittedMatrices;
	protected final RenderType renderType;
	protected final List<Quad> submittedQuads;
	protected final float textureWidth;
	protected final float textureHeight;
	protected final int light;
	protected final boolean submitted;

	protected record Quad(float x1, float x2, float y1, float y2, float z, float u1, float u2, float v1, float v2, int argb) {
	}

	public static boolean areWeShadersRightNow() {
		try {
			Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			Method instanceMethod = apiClass.getDeclaredMethod("getInstance");
			Method inUseMethod = apiClass.getDeclaredMethod("isShaderPackInUse");
			Object apiInstance = instanceMethod.invoke(null);
			return (boolean) inUseMethod.invoke(apiInstance);
		} catch (Exception e) {
			return false;
		}
	}

	public static void drawSingle(PoseStack matrices, SubmitNodeCollector submitter, Identifier texture, int textureWidth, int textureHeight, int light, int x, int y, float z, int width, int height, int u, int v, int regionWidth, int regionHeight, int argb, boolean drawingTransparent) {
		try (DrawBatcher batcher = new DrawBatcher(matrices, submitter, texture, textureWidth, textureHeight, light, drawingTransparent)) {
			batcher.add(x, y, z, width, height, u, v, regionWidth, regionHeight, argb);
		}
	}

	public static void drawSingle(PoseStack matrices, Identifier texture, int textureWidth, int textureHeight, int light, int x, int y, float z, int width, int height, int u, int v, int regionWidth, int regionHeight, int argb, boolean drawingTransparent) {
		try (DrawBatcher batcher = new DrawBatcher(matrices, (SubmitNodeCollector) null, texture, textureWidth, textureHeight, light, drawingTransparent)) {
			batcher.add(x, y, z, width, height, u, v, regionWidth, regionHeight, argb);
		}
	}

	public DrawBatcher(PoseStack matrices, SubmitNodeCollector submitter, Identifier texture, int textureWidth, int textureHeight, int light, boolean drawingTransparent) {
		this.submitter = submitter;
		this.submitted = submitter != null;
		if (submitter == null) {
			this.renderType = RenderTypes.text(texture);
			this.bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
			this.vertexConsumer = bufferBuilder;
			this.submittedMatrices = null;
			this.submittedQuads = null;
		} else {
			this.renderType = areWeShadersRightNow() ? (drawingTransparent ? RenderTypes.entityTranslucent(texture) : RenderTypes.entitySolid(texture)) : RenderTypes.text(texture);
			this.bufferBuilder = null;
			this.vertexConsumer = null;
			this.submittedMatrices = new PoseStack();
			this.submittedMatrices.last().set(matrices.last());
			this.submittedQuads = new ArrayList<>();
		}
		this.matrix4f = matrices.last().pose();
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
		this.light = light;
	}

	public void add(int x, int y, float z, int width, int height, int u, int v, int regionWidth, int regionHeight, int argb) {
		this.innerAdd(x, x + width, y, y + height, z,
			(u + 0.0F) / textureWidth,
			(u + (float) regionWidth) / textureWidth,
			(v + 0.0F) / textureHeight,
			(v + (float) regionHeight) / textureHeight,
			argb
		);
	}

	protected void innerAdd(float x1, float x2, float y1, float y2, float z, float u1, float u2, float v1, float v2, int argb) {
		if (submitted) {
			submittedQuads.add(new Quad(x1, x2, y1, y2, z, u1, u2, v1, v2, argb));
			return;
		}
		vertexConsumer.addVertex(matrix4f, x1, y1, z).setColor(argb).setUv(u1, v1).setLight(light);
		vertexConsumer.addVertex(matrix4f, x1, y2, z).setColor(argb).setUv(u1, v2).setLight(light);
		vertexConsumer.addVertex(matrix4f, x2, y2, z).setColor(argb).setUv(u2, v2).setLight(light);
		vertexConsumer.addVertex(matrix4f, x2, y1, z).setColor(argb).setUv(u2, v1).setLight(light);
	}

	@Override
	public void close() {
		if (submitted) {
			if (!submittedQuads.isEmpty()) {
				submitter.submitCustomGeometry(submittedMatrices, renderType, (pose, vertexConsumer) -> {
					Matrix4f matrix = pose.pose();
					for (Quad quad : submittedQuads) {
						vertexConsumer.addVertex(matrix, quad.x1, quad.y1, quad.z).setColor(quad.argb).setUv(quad.u1, quad.v1).setLight(light);
						vertexConsumer.addVertex(matrix, quad.x1, quad.y2, quad.z).setColor(quad.argb).setUv(quad.u1, quad.v2).setLight(light);
						vertexConsumer.addVertex(matrix, quad.x2, quad.y2, quad.z).setColor(quad.argb).setUv(quad.u2, quad.v2).setLight(light);
						vertexConsumer.addVertex(matrix, quad.x2, quad.y1, quad.z).setColor(quad.argb).setUv(quad.u2, quad.v1).setLight(light);
					}
				});
			}
			return;
		}
		if (bufferBuilder != null) {
			MeshData bb = bufferBuilder.build();
			if (bb != null) renderType.draw(bb);
		}
	}
}
