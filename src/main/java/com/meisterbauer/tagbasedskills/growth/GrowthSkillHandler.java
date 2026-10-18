package com.meisterbauer.tagbasedskills.growth;

import com.meisterbauer.tagbasedskills.TagBasedSkills;
import com.meisterbauer.tagbasedskills.config.CropGrowthConfig;
import com.meisterbauer.tagbasedskills.config.CropGrowthConfig.GrowthType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/*
 * Applies tag-gated crop growth effects around crouching or sprinting players.
 * The handler runs only on the logical server, once per configured activation interval.
 */
@EventBusSubscriber(modid = TagBasedSkills.MOD_ID)
public final class GrowthSkillHandler {
    private static final String TWERK_GROWTH_TAG_NAME = "twerk_growth";
    private static final String SPRINT_GROWTH_TAG_NAME = "sprint_growth";
    private static final TagKey<Block> TWERK_GROWTH_TAG = BlockTags.create(ResourceLocation.fromNamespaceAndPath(TagBasedSkills.MOD_ID, TWERK_GROWTH_TAG_NAME));
    private static final TagKey<Block> SPRINT_GROWTH_TAG = BlockTags.create(ResourceLocation.fromNamespaceAndPath(TagBasedSkills.MOD_ID, SPRINT_GROWTH_TAG_NAME));
    private static final int BASE_RADIUS = 1;
    // Maximum horizontal distance, in blocks, that either growth skill may scan from a player.
    private static final int MAXIMUM_RADIUS = 16;
    private static final int BASE_HEIGHT = 1;
    private static final int BASE_MAX_BLOCKS = 25;
    private static final int ACTIVATION_INTERVAL_TICKS = 20;
    private static final double BASE_GROWTH_CHANCE = 0.1D;

    //Checks active players at a fixed cadence, then evaluates each applicable skill separately.
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level) || level.getGameTime() % ACTIVATION_INTERVAL_TICKS != 0) {
            return;
        }

        if (player.isCrouching()) {
            applyGrowth(player, level, GrowthType.TWERK, TWERK_GROWTH_TAG);
        }
        if (player.isSprinting()) {
            applyGrowth(player, level, GrowthType.SPRINT, SPRINT_GROWTH_TAG);
        }
    }

    //Resolves a skill's configured upgrades from player tags and starts its area-of-effect pass.
    private static void applyGrowth(Player player, ServerLevel level, GrowthType growthType, TagKey<Block> growthTag) {
        if (!CropGrowthConfig.isUnlocked(player.getTags(), growthType)) {
            return;
        }

        int radius = Math.min(
                BASE_RADIUS + CropGrowthConfig.radiusBonus(player.getTags(), growthType),
                MAXIMUM_RADIUS
        );
        double powerMultiplier = CropGrowthConfig.powerMultiplier(player.getTags(), growthType);
        int maxBlocks = CropGrowthConfig.upgradedMaxBlocks(BASE_MAX_BLOCKS, powerMultiplier);
        double chance = CropGrowthConfig.upgradedChance(BASE_GROWTH_CHANCE, powerMultiplier);
        growNearbyBlocks(level, player.blockPosition(), growthTag, radius, maxBlocks, chance);
    }

    /*
     * Visits the rectangular area around the player until the maximum number of successful growth
     * attempts has been reached. An eligible block must also pass the per-block chance roll.
     */
    private static void growNearbyBlocks(
            ServerLevel level,
            BlockPos origin,
            TagKey<Block> growthTag,
            int radius,
            int maxBlocks,
            double chance
    ) {
        RandomSource random = level.getRandom();
        int affectedBlocks = 0;
        for (BlockPos position : BlockPos.betweenClosed(
                origin.offset(-radius, -BASE_HEIGHT, -radius),
                origin.offset(radius, BASE_HEIGHT, radius)
        )) {
            if (affectedBlocks >= maxBlocks) {
                return;
            }

            BlockState state = level.getBlockState(position);
            if (!isGrowthTarget(state, growthTag) || random.nextDouble() > chance) {
                continue;
            }

            if (growBlock(level, position, state, random)) {
                affectedBlocks++;
            }
        }
    }

    /*
     * Recognizes plants through their bonemeal interface or the Minecraft crop block tag, so modded
     * plants do not have to extend a specific crop class. Skill-specific tags also allow plants
     * such as cactus and sugar cane that grow through random ticks without supporting bonemeal.
     * Random ticking alone is not a plant marker: unrelated blocks also use it.
     */
    private static boolean isGrowthTarget(BlockState state, TagKey<Block> growthTag) {
        return state.getBlock() instanceof BonemealableBlock
                || state.is(BlockTags.CROPS)
                || state.is(growthTag);
    }

    /*
     * Performs normal bonemeal behavior when supported; otherwise invokes a random tick for blocks
     * that use random ticking (for example, cactus and sugar cane).
     * @return whether this block consumed one of the skill's affected-block slots
     */
    private static boolean growBlock(ServerLevel level, BlockPos position, BlockState state, RandomSource random) {
        Block block = state.getBlock();
        if (block instanceof BonemealableBlock bonemealable
                && bonemealable.isValidBonemealTarget(level, position, state)
                && bonemealable.isBonemealSuccess(level, random, position, state)) {
            bonemealable.performBonemeal(level, random, position, state);
            return true;
        }

        if (state.isRandomlyTicking()) {
            state.randomTick(level, position, random);
            return true;
        }
        return false;
    }
}
