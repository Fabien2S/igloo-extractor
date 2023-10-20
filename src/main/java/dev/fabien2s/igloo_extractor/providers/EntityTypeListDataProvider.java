package dev.fabien2s.igloo_extractor.providers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.fabien2s.igloo_extractor.mixin.EntityTypeAccessor;
import dev.fabien2s.igloo_extractor.mixin.FeatureFlagSetAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class EntityTypeListDataProvider implements DataProvider {

	private final PackOutput output;

	public EntityTypeListDataProvider(PackOutput output) {
		this.output = output;
	}

	@Override
	public @NotNull CompletableFuture<?> run(CachedOutput cachedOutput) {
		JsonObject rootObject = new JsonObject();

		JsonArray categoryArray = new JsonArray();
		for (MobCategory category : MobCategory.values()) {
			String categoryName = category.getSerializedName();
			categoryArray.add(categoryName);
		}
		rootObject.add("categories", categoryArray);

		JsonObject entityListObject = new JsonObject();
		for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
			JsonObject entityObject = new JsonObject();

			entityObject.addProperty("id", BuiltInRegistries.ENTITY_TYPE.getId(entityType));
			entityObject.addProperty("update_rate", entityType.updateInterval());
			entityObject.addProperty("tracking_range", entityType.clientTrackingRange());

			MobCategory category = entityType.getCategory();
			entityObject.addProperty("category_id", category.ordinal());

			JsonObject spawnRulesObject = new JsonObject();
			{
				spawnRulesObject.addProperty("max_instances_per_chunk", category.getMaxInstancesPerChunk());
				spawnRulesObject.addProperty("max_distance", category.getDespawnDistance());
			}
			entityObject.add("spawn_rules", spawnRulesObject);

			JsonObject flagsObject = new JsonObject();
			{
				flagsObject.addProperty("can_serialize", entityType.canSerialize());
				flagsObject.addProperty("can_summon", entityType.canSummon());
				flagsObject.addProperty("can_spawn_far_from_player", entityType.canSpawnFarFromPlayer());

				flagsObject.addProperty("is_friendly", category.isFriendly());
				flagsObject.addProperty("is_persistent", category.isPersistent());
				flagsObject.addProperty("is_fire_immune", entityType.fireImmune());
			}
			entityObject.add("flags", flagsObject);

			{
				FeatureFlagSet featureFlagSet = entityType.requiredFeatures();
				long featureFlagMask = ((FeatureFlagSetAccessor) (Object) featureFlagSet).getMask();
				entityObject.addProperty("required_features", featureFlagMask);
			}

			JsonArray blockImmunityArray = new JsonArray();
			{
				for (Block block : ((EntityTypeAccessor) entityType).getImmuneTo()) {
					ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(block);
					blockImmunityArray.add(blockKey.toString());
				}
			}
			entityObject.add("immune_to", blockImmunityArray);

			JsonObject sizeObject = new JsonObject();
			{
				EntityDimensions entityDimensions = entityType.getDimensions();
				sizeObject.addProperty("width", entityDimensions.width);
				sizeObject.addProperty("height", entityDimensions.height);
				sizeObject.addProperty("is_fixed", entityDimensions.fixed);
			}
			entityObject.add("size", sizeObject);

			ResourceLocation entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
			entityListObject.add(entityKey.toString(), entityObject);
		}
		rootObject.add("entity_types", entityListObject);

		Path path = this.output.getOutputFolder(PackOutput.Target.REPORTS).resolve("entities.json");
		return DataProvider.saveStable(cachedOutput, rootObject, path);
	}

	@Override
	public @NotNull String getName() {
		return "Entity Types";
	}

}
