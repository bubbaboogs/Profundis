package firenh.profundis.features.features;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;

import firenh.profundis.features.features.config.LargeOreFeatureConfig;
import net.minecraft.block.BlockState;
import net.minecraft.structure.rule.RuleTest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.noise.InterpolatedNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.densityfunction.DensityFunction.UnblendedNoisePos;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class LargeOreFeature extends Feature<LargeOreFeatureConfig> {

    public LargeOreFeature(Codec<LargeOreFeatureConfig> configCodec) {
        super(configCodec);
    }

    protected Optional<BlockState> getBlockState(StructureWorldAccess world, BlockPos pos, BlockState currentState, Random random, List<OreFeatureConfig.Target> targets) {
        for (OreFeatureConfig.Target t : targets) {
            RuleTest rule = t.target;
            
            if (rule.test(currentState, random)) {
                return Optional.of(t.state);
            }
        }

        return Optional.empty();
    }

    @Override
    public boolean generate(FeatureContext<LargeOreFeatureConfig> context) {
        Random random = context.getRandom();
        LargeOreFeatureConfig config = context.getConfig();
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        boolean returnVal = false;
        
        List<OreFeatureConfig.Target> targets = config.targets();
           int radius = config.radius().get(random);
          double scale = config.scale();
          double factor = config.factor();
         double smearing = config.smearing();
        double valueRange = config.valueRange();
        double valueOffset = config.valueOffset();
         boolean bordersAir = config.bordersAir();

        ChunkPos originChunkPos = new ChunkPos(origin.getX() / 16, origin.getZ() / 16);
        InterpolatedNoiseSampler noise = new InterpolatedNoiseSampler(random, scale, scale, factor, factor, smearing);

        Iterator<BlockPos> iter = BlockPos.iterateOutwards(origin, radius, radius, radius).iterator();

        while (iter.hasNext()) {
            BlockPos pos = iter.next();
            if ((!origin.isWithinDistance(pos, radius))) continue;

            if (Math.abs(originChunkPos.x - (pos.getX() / 16)) > 1 || Math.abs(originChunkPos.z - (pos.getZ() / 16)) > 1) continue; 

            double value = noise.sample(new UnblendedNoisePos(pos.getX(), pos.getY(), pos.getZ()));
            double distance = Math.sqrt(origin.getSquaredDistance(pos));
            double checkVal = value + (distance / radius);

            // if (world.isValidForSetBlock(pos)) System.out.println("valid");

            if (Math.abs(checkVal - valueOffset) < valueRange && world.isValidForSetBlock(pos)) {
                boolean hasAir = false;

                if (bordersAir) {
                    for (Direction d : Direction.values()) {
                        if (!world.getBlockState(pos.offset(d)).isOpaque()) {
                            // System.out.println("air");
                            hasAir = true;
                            break;
                        }
                    }
                } else {
                    hasAir = true;
                }

                if (hasAir) {
                    BlockState currentState = world.getBlockState(pos);
                    Optional<BlockState> newState = getBlockState(world, pos, currentState, random, targets);

                    if (newState.isPresent()) {
                        this.setBlockState(world, pos, newState.get());
                        returnVal = true;
                    }

                }
            }
        }

        return returnVal;
    }
    
}
