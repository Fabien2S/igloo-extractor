package dev.fabien2s.igloo_extractor.providers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class BlockTypeListDataProvider implements DataProvider {

    private final PackOutput output;

    public BlockTypeListDataProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public @NotNull CompletableFuture<?> run(CachedOutput cachedOutput) {
        JsonObject rootObject = new JsonObject();

        JsonObject blockListObject = new JsonObject();
        for (Block block : BuiltInRegistries.BLOCK) {
            StateDefinition<Block, BlockState> stateDefinition = block.getStateDefinition();

            JsonObject blockObject = new JsonObject();
            {
                blockObject.addProperty("id", BuiltInRegistries.BLOCK.getId(block));

                {
                    BlockState defaultBlockState = block.defaultBlockState();
                    blockObject.addProperty("default_state_id", Block.getId(defaultBlockState));
                }

                blockObject.addProperty("friction", block.getFriction());
                blockObject.addProperty("speed_factor", block.getSpeedFactor());
                blockObject.addProperty("jump_factor", block.getJumpFactor());
                blockObject.addProperty("explosion_resistance", block.getExplosionResistance());

                JsonArray propertyContainerObject = new JsonArray();
                {
                    for (Property<?> property : stateDefinition.getProperties()) {
                        String propertyName = property.getName();
                        propertyContainerObject.add(propertyName);
                    }
                }
                blockObject.add("properties", propertyContainerObject);

                JsonArray stateContainerArray = new JsonArray();
                {
                    for (BlockState blockState : stateDefinition.getPossibleStates()) {
                        JsonObject stateObject = new JsonObject();
                        {
                            stateObject.addProperty("id", Block.getId(blockState));

                            JsonArray blockStatePropertyArray = new JsonArray();
                            {
                                for (Property<?> property : blockState.getProperties()) {
                                    Comparable<?> propertyValue = blockState.getValue(property);
                                    String propertyName = Util.getPropertyName(property, propertyValue);
                                    blockStatePropertyArray.add(propertyName);
                                }
                            }
                            stateObject.add("properties", blockStatePropertyArray);

                            JsonObject shapeObject = new JsonObject();
                            {
                                JsonArray collisionShapeArray = serializeBlockShape(ClipContext.Block.COLLIDER, blockState);
                                shapeObject.add("collision", collisionShapeArray);

                                JsonArray outlineShapeArray = serializeBlockShape(ClipContext.Block.OUTLINE, blockState);
                                shapeObject.add("outline", outlineShapeArray);
                            }
                            stateObject.add("shape", shapeObject);
                        }
                        stateContainerArray.add(stateObject);
                    }
                }
                blockObject.add("states", stateContainerArray);
            }

            ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(block);
            blockListObject.add(blockKey.toString(), blockObject);
        }
        rootObject.add("block_types", blockListObject);

        Path path = this.output.getOutputFolder(PackOutput.Target.REPORTS).resolve("blocks.json");
        return DataProvider.saveStable(cachedOutput, rootObject, path);
    }

    @Override
    public @NotNull String getName() {
        return "Block Types";
    }

    private JsonObject serializeVector(double x, double y, double z) {
        JsonObject vectorObject = new JsonObject();
        vectorObject.addProperty("x", x);
        vectorObject.addProperty("y", y);
        vectorObject.addProperty("z", z);
        return vectorObject;
    }

    private JsonArray serializeBlockShape(ClipContext.Block blockContext, BlockState blockState) {
        JsonArray shapeArray = new JsonArray();

        VoxelShape shape = blockContext.get(blockState, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());

        for (AABB aabb : shape.toAabbs()) {
            JsonObject aabbObject = new JsonObject();
            aabbObject.add("min", serializeVector(aabb.minX, aabb.minY, aabb.minZ));
            aabbObject.add("max", serializeVector(aabb.maxX, aabb.maxY, aabb.maxZ));
            shapeArray.add(aabbObject);
        }

        return shapeArray;
    }

}

