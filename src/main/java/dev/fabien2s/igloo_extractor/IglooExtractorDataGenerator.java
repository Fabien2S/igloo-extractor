package dev.fabien2s.igloo_extractor;

import dev.fabien2s.igloo_extractor.providers.*;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.data.DataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IglooExtractorDataGenerator implements DataGeneratorEntrypoint {

    public static final String ID = "igloo_extractor";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator dataGenerator) {
        FabricDataGenerator.Pack pack = dataGenerator.createPack();

        this.registerSimpleProvider(pack);
        this.registerRegistryProvider(pack);
    }

    private void registerSimpleProvider(DataGenerator.PackGenerator pack) {
        pack.addProvider(BlockTypeListDataProvider::new);
        pack.addProvider(EntityTypeListDataProvider::new);
        pack.addProvider(FeatureFlagsDataProvider::new);
        pack.addProvider(ProtocolDataProvider::new);
        pack.addProvider(VersionInfoDataProvider::new);
    }

    private void registerRegistryProvider(FabricDataGenerator.Pack pack) {
        pack.addProvider(RegistryPayloadDataProvider::new);
    }

}
