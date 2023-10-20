package dev.fabien2s.igloo_extractor.providers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.fabien2s.igloo_extractor.IglooExtractorDataGenerator;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class FeatureFlagsDataProvider implements DataProvider {

    private final PackOutput output;

    public FeatureFlagsDataProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public @NotNull CompletableFuture<?> run(CachedOutput cachedOutput) {
        JsonObject rootObject = new JsonObject();

        rootObject.add("feature_flags", serializeFeatureFlagSet(FeatureFlags.REGISTRY.allFlags()));
        rootObject.add("default_flags", serializeFeatureFlagSet(FeatureFlags.DEFAULT_FLAGS));
        rootObject.add("vanilla_flags", serializeFeatureFlagSet(FeatureFlags.VANILLA_SET));

        Path path = this.output.getOutputFolder(PackOutput.Target.REPORTS).resolve("features.json");
        return DataProvider.saveStable(cachedOutput, rootObject, path);
    }

    @Override
    public @NotNull String getName() {
        return "Feature Flags";
    }

    private static JsonElement serializeFeatureFlagSet(FeatureFlagSet featureFlagSet) {
        return FeatureFlags.CODEC.encodeStart(JsonOps.INSTANCE, featureFlagSet).getOrThrow(false, s ->
                IglooExtractorDataGenerator.LOGGER.error("Failed to get feature flag")
        );
    }

}
