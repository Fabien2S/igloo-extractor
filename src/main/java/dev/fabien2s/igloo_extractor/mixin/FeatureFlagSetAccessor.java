package dev.fabien2s.igloo_extractor.mixin;

import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlagUniverse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FeatureFlagSet.class)
public interface FeatureFlagSetAccessor {

    @Accessor
    FeatureFlagUniverse getUniverse();

    @Accessor
    long getMask();

}
