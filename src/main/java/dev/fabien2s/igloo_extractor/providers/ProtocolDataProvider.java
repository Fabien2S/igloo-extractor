package dev.fabien2s.igloo_extractor.providers;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class ProtocolDataProvider implements DataProvider {

    private final PackOutput output;

    public ProtocolDataProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public @NotNull CompletableFuture<?> run(CachedOutput cachedOutput) {
        JsonObject rootObject = new JsonObject();

        JsonObject stateListObj = new JsonObject();
        for (ConnectionProtocol protocol : ConnectionProtocol.values()) {

            JsonObject protocolObject = new JsonObject();
            {
                protocolObject.addProperty("id", protocol.id());
                protocolObject.add("to_client", serializeProtocolFlow(protocol, PacketFlow.CLIENTBOUND));
                protocolObject.add("to_server", serializeProtocolFlow(protocol, PacketFlow.SERVERBOUND));
            }

            String protocolName = protocol.name().toLowerCase(Locale.ROOT);
            stateListObj.add(protocolName, protocolObject);
        }
        rootObject.add("states", stateListObj);

        Path path = this.output.getOutputFolder(PackOutput.Target.REPORTS).resolve("protocol.json");
        return DataProvider.saveStable(cachedOutput, rootObject, path);
    }

    @Override
    public @NotNull String getName() {
        return "Packet Types";
    }

    private static JsonObject serializeProtocolFlow(ConnectionProtocol protocol, PacketFlow flow) {

        // Do not remove flow prefix for Handshaking state
        boolean hasPrefix = protocol != ConnectionProtocol.HANDSHAKING;
        String prefix = hasPrefix ? flow.name().toLowerCase(Locale.ROOT) : "";
        String suffix = "Packet".toLowerCase(Locale.ROOT);

        JsonObject packetTypesObj = new JsonObject();
        {
            Int2ObjectMap<Class<? extends Packet<?>>> packets = protocol.getPacketsByIds(flow);
            packets.forEach((packetId, packetClass) -> {
                String packetName = packetClass.getName();

                // Remove package from class name
                // Class.getSimpleName() breaks with inner class (such as MoveEntityPacket$Pos)
                int lastPackageSeparatorIndex = packetName.lastIndexOf('.');
                packetName = packetName.substring(lastPackageSeparatorIndex + 1);

                // Checks if the class name contains the packet flow prefix
                if (packetName.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    // Remove it
                    packetName = packetName.substring(prefix.length());
                } else {
                    // Otherwise, prepend an underscore to indicate "system packet" (such as BundleDelimiterPacket)
                    packetName = "_" + packetName;
                }

                if(packetName.toLowerCase(Locale.ROOT).endsWith(suffix)) {
                    packetName = packetName.substring(0, packetName.length() - suffix.length());
                }

                packetTypesObj.addProperty(packetName, packetId);
            });
        }
        return packetTypesObj;
    }

}
