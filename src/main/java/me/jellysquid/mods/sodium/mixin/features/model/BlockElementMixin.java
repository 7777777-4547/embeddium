package me.jellysquid.mods.sodium.mixin.features.model;

import com.mojang.math.Vector3f;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.renderer.block.model.BlockElement;
import org.embeddedt.embeddium.util.PlatformUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BlockElement.class)
public class BlockElementMixin {
    @ModifyVariable(method = "<init>",
        at = @At("HEAD"), argsOnly = true, index = 1)
    private static Vector3f epsilonizeFrom(Vector3f vector) {
        return embeddium$epsilonize(vector);
    }

    @ModifyVariable(method = "<init>",
            at = @At("HEAD"), argsOnly = true, index = 2)
    private static Vector3f epsilonizeTo(Vector3f vector) {
        return embeddium$epsilonize(vector);
    }

    private static Vector3f embeddium$epsilonize(Vector3f v) {
        if (v == null || !PlatformUtil.isLoadValid()  || !SodiumClientMod.options().advanced.useCompactVertexFormat) {
            return v;
        }
        v.setX(embeddium$epsilonize(v.x()));
        v.setY(embeddium$epsilonize(v.y()));
        v.setZ(embeddium$epsilonize(v.z()));
        return v;
    }

    private static final float EMBEDDIUM$MINIMUM_EPSILON = 0.008f;

    private static float embeddium$epsilonize(float f) {
        int roundedCoord = Math.round(f);
        float difference = f - roundedCoord;
        // Ignore components that are integers or far enough away from a texel for epsilonizing to be unnecessary
        if(difference == 0 || Math.abs(difference) >= EMBEDDIUM$MINIMUM_EPSILON) {
            return f;
        } else {
            // "Push" the coordinate slightly further away from the texel so z-fighting is less likely
            // This maps e.g. 0.001 -> 0.01, which should be enough
            return roundedCoord + (difference * 10);
        }
    }
}
