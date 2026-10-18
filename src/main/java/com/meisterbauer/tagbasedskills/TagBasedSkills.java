package com.meisterbauer.tagbasedskills;

import com.meisterbauer.tagbasedskills.config.EnchantmentConfig;
import com.meisterbauer.tagbasedskills.config.CropGrowthConfig;
import net.neoforged.fml.common.Mod;

@Mod(TagBasedSkills.MOD_ID)
public final class TagBasedSkills {

    //Namespace used for registries, tags, configuration paths, and NeoForge discovery.
    public static final String MOD_ID = "tag_based_skills";

    //NeoForge entry point. Configuration is loaded before any gameplay event can use it.
    public TagBasedSkills() {
        EnchantmentConfig.initialize();
        CropGrowthConfig.initialize();
    }
}
