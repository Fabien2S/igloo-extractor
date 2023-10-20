package dev.fabien2s.igloo_extractor.providers;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import dev.fabien2s.igloo_extractor.IglooExtractorDataGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.server.RegistryLayer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class RegistryPayloadDataProvider implements DataProvider {

    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public RegistryPayloadDataProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.output = output;
        this.registries = registries;
    }

    @Override
    public @NotNull CompletableFuture<?> run(CachedOutput cachedOutput) {
        return this.registries.thenAccept(provider -> {

            LayeredRegistryAccess<RegistryLayer> layeredRegistryAccess = RegistryLayer.createRegistryAccess();
            Stream<RegistryAccess.RegistryEntry<?>> networkedRegistries = RegistrySynchronization.networkedRegistries(layeredRegistryAccess);
            RegistryAccess.Frozen networkedRegistryAccess = new RegistryAccess.ImmutableRegistryAccess(networkedRegistries).freeze();
            ClientboundRegistryDataPacket registryPayloadPacket = new ClientboundRegistryDataPacket(networkedRegistryAccess);

            ByteBuf buffer = Unpooled.buffer();
            {
                FriendlyByteBuf friendlyBuffer = new FriendlyByteBuf(buffer);
                registryPayloadPacket.write(friendlyBuffer);
            }

            Path path = this.output.getOutputFolder(PackOutput.Target.REPORTS).resolve("payload.nbt");
            byte[] payloadBytes = ByteBufUtil.getBytes(buffer);
            HashCode payloadHash = Hashing.murmur3_128().hashBytes(payloadBytes);
            try {
                cachedOutput.writeIfNeeded(path, payloadBytes, payloadHash);
            } catch (IOException e) {
                IglooExtractorDataGenerator.LOGGER.error("Failed to write registry payload", e);
            }
        });
    }

    @Override
    public @NotNull String getName() {
        return "Registry Payload";
    }

}
