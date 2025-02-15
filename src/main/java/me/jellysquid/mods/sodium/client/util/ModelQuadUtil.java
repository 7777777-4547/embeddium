package me.jellysquid.mods.sodium.client.util;

import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * Provides some utilities and constants for interacting with vanilla's model quad vertex format.
 *
 * This is the current vertex format used by Minecraft for chunk meshes and model quads. Internally, it uses integer
 * arrays for store baked quad data, and as such the following table provides both the byte and int indices.
 *
 * Byte Index    Integer Index             Name                 Format                 Fields
 * 0 ..11        0..2                      Position             3 floats               x, y, z
 * 12..15        3                         Color                4 unsigned bytes       a, r, g, b
 * 16..23        4..5                      Block Texture        2 floats               u, v
 * 24..27        6                         Light Texture        2 shorts               u, v
 * 28..30        7                         Normal               3 unsigned bytes       x, y, z
 * 31                                      Padding              1 byte
 */
public class ModelQuadUtil {
    // Integer indices for vertex attributes, useful for accessing baked quad data
    public static final int POSITION_INDEX = 0,
            COLOR_INDEX = 3,
            TEXTURE_INDEX = 4,
            LIGHT_INDEX = 6,
            NORMAL_INDEX = 7;

    // Size of vertex format in 4-byte integers
    public static final int VERTEX_SIZE = 8;
    public static final int VERTEX_SIZE_BYTES = VERTEX_SIZE * 4;

    // Cached array of normals for every facing to avoid expensive computation
    static final int[] NORMALS = new int[DirectionUtil.ALL_DIRECTIONS.length];

    static {
        for (int i = 0; i < NORMALS.length; i++) {
            NORMALS[i] = Norm3b.pack(DirectionUtil.ALL_DIRECTIONS[i].getNormal());
        }
    }

    /**
     * Returns the normal vector for a model quad with the given {@param facing}.
     */
    public static int getFacingNormal(Direction facing) {
        return NORMALS[facing.ordinal()];
    }

    public static int getFacingNormal(Direction facing, int bakedNormal) {
        if(!hasNormal(bakedNormal))
            return NORMALS[facing.ordinal()];
        return bakedNormal;
    }

    public static boolean hasNormal(int n) {
        return (n & 0xFFFFFF) != 0;
    }

    /**
     * @param vertexIndex The index of the vertex to access
     * @return The starting offset of the vertex's attributes
     */
    public static int vertexOffset(int vertexIndex) {
        return vertexIndex * VERTEX_SIZE;
    }

    public static int mergeBakedLight(int packedLight, int calcLight) {
        // bail early in most cases
        if (packedLight == 0)
            return calcLight;

        int psl = (packedLight >> 16) & 0xFF;
        int csl = (calcLight >> 16) & 0xFF;
        int pbl = (packedLight) & 0xFF;
        int cbl = (calcLight) & 0xFF;
        int bl = Math.max(pbl, cbl);
        int sl = Math.max(psl, csl);
        return (sl << 16) | bl;
    }

    /**
     * Mixes two ABGR colors together like what Forge does in VertexConsumer.
     *
     * Despite the name, the method tries to avoid doing any work whenever possible.
     */
    public static int mixABGRColors(int colorA, int colorB) {
        // Most common case: Either quad coloring or tint-based coloring, but not both
        if (colorA == -1) {
            return colorB;
        } else if (colorB == -1) {
            return colorA;
        }
        // General case (rare): Both colorings, actually perform the multiplication
        int a = (int)((ColorABGR.unpackAlpha(colorA)/255.0f) * (ColorABGR.unpackAlpha(colorB)/255.0f) * 255.0f);
        int b = (int)((ColorABGR.unpackBlue(colorA)/255.0f) * (ColorABGR.unpackBlue(colorB)/255.0f) * 255.0f);
        int g = (int)((ColorABGR.unpackGreen(colorA)/255.0f) * (ColorABGR.unpackGreen(colorB)/255.0f) * 255.0f);
        int r = (int)((ColorABGR.unpackRed(colorA)/255.0f) * (ColorABGR.unpackRed(colorB)/255.0f) * 255.0f);
        return ColorABGR.pack(r, g, b, a);
    }

    public static ModelQuadFacing findNormalFace(float x, float y, float z) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            return ModelQuadFacing.UNASSIGNED;
        }

        float maxDot = 0;
        Direction closestFace = null;

        for (Direction face : DirectionUtil.ALL_DIRECTIONS) {
            float dot = (x * face.getStepX()) + (y * face.getStepY()) + (z * face.getStepZ());

            if (dot > maxDot) {
                maxDot = dot;
                closestFace = face;
            }
        }

        if (closestFace != null && Mth.equal(maxDot, 1.0f)) {
            return ModelQuadFacing.fromDirection(closestFace);
        }

        return ModelQuadFacing.UNASSIGNED;
    }

    public static ModelQuadFacing findNormalFace(int normal) {
        return findNormalFace(Norm3b.unpackX(normal), Norm3b.unpackY(normal), Norm3b.unpackZ(normal));
    }

    public static int calculateNormal(ModelQuadView quad) {
        final float x0 = quad.getX(0);
        final float y0 = quad.getY(0);
        final float z0 = quad.getZ(0);

        final float x1 = quad.getX(1);
        final float y1 = quad.getY(1);
        final float z1 = quad.getZ(1);

        final float x2 = quad.getX(2);
        final float y2 = quad.getY(2);
        final float z2 = quad.getZ(2);

        final float x3 = quad.getX(3);
        final float y3 = quad.getY(3);
        final float z3 = quad.getZ(3);

        final float dx0 = x2 - x0;
        final float dy0 = y2 - y0;
        final float dz0 = z2 - z0;
        final float dx1 = x3 - x1;
        final float dy1 = y3 - y1;
        final float dz1 = z3 - z1;

        float normX = dy0 * dz1 - dz0 * dy1;
        float normY = dz0 * dx1 - dx0 * dz1;
        float normZ = dx0 * dy1 - dy0 * dx1;

        float l = (float) Math.sqrt(normX * normX + normY * normY + normZ * normZ);

        if (l != 0) {
            normX /= l;
            normY /= l;
            normZ /= l;
        }

        return Norm3b.pack(normX, normY, normZ);
    }
}
