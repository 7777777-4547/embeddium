package me.jellysquid.mods.sodium.client.render;

import org.lwjgl.system.MemoryStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import java.nio.FloatBuffer;

public class GameRendererContext {
    private static Matrix4f PROJECTION_MATRIX;

    public static void captureProjectionMatrix(Matrix4f matrix) {
        PROJECTION_MATRIX = matrix.copy();
    }

    /**
     * Obtains a model-view-projection matrix by multiplying the projection matrix with the model-view matrix
     * from {@param matrices}.
     *
     * The returned buffer is only valid for the lifetime of {@param stack}.
     *
     * @return A float-buffer on the stack containing the model-view-projection matrix in a format suitable for
     * uploading as uniform state
     */
    public static FloatBuffer getModelViewProjectionMatrix(PoseStack.Pose matrices, MemoryStack memoryStack) {
        if (PROJECTION_MATRIX == null) {
            throw new IllegalStateException("Projection matrix has not been captured");
        }

        FloatBuffer bufModelViewProjection = memoryStack.mallocFloat(16);

        Matrix4f matrix = PROJECTION_MATRIX.copy();
        matrix.multiply(matrices.pose());
        matrix.store(bufModelViewProjection);

        return bufModelViewProjection;
    }
}
