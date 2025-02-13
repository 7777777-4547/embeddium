package me.jellysquid.mods.sodium.mixin.core.model;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.world.biome.BlockColorsExtended;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockColors.class)
public class MixinBlockColors implements BlockColorsExtended {
    private Reference2ReferenceMap<Block, BlockColor> blocksToColor;

    private static final BlockColor DEFAULT_PROVIDER = (state, view, pos, tint) -> -1;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        this.blocksToColor = new Reference2ReferenceOpenHashMap<>();
        this.blocksToColor.defaultReturnValue(DEFAULT_PROVIDER);
    }

    @Inject(method = "register", at = @At("HEAD"))
    private void preRegisterColor(BlockColor provider, Block[] blocks, CallbackInfo ci) {
        // Synchronize because Forge mods register this without enqueuing the call on the main thread
        // and then blame Embeddium for the crash because of the mixin, despite vanilla using a non-concurrent
        // HashMap too
        synchronized (this.blocksToColor) {
            for (Block block : blocks) {
                if(provider != null)
                    this.blocksToColor.put(block, provider);
            }
        }
    }

    @Override
    public BlockColor getColorProvider(BlockState state) {
        return this.blocksToColor.get(state.getBlock());
    }
}
