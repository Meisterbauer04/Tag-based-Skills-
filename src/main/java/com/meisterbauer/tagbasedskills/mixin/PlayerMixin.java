package com.meisterbauer.tagbasedskills.mixin;

import com.meisterbauer.tagbasedskills.config.EnchantmentConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/*
 * Adjusts the level cost charged after the player selects an enchanting-table option.
 * The mixin leaves non-enchanting menus and invalid option indices unchanged.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
    private static final String ON_ENCHANTMENT_PERFORMED_METHOD = "onEnchantmentPerformed";
    private static final String INJECTION_HEAD = "HEAD";

    @ModifyVariable(method = ON_ENCHANTMENT_PERFORMED_METHOD, at = @At(INJECTION_HEAD), argsOnly = true, ordinal = 0)
    private int tagBasedSkills$modifyEnchantingCost(int vanillaCost) {
        Player player = (Player) (Object) this;
        if (!(player.containerMenu instanceof EnchantmentMenu menu)) {
            return vanillaCost;
        }

        // Vanilla passes the selected option as the one-based 1/2/3 level cost.
        int enchantmentOptionIndex = vanillaCost - 1;
        if (enchantmentOptionIndex < 0 || enchantmentOptionIndex >= menu.costs.length) {
            return vanillaCost;
        }

        return EnchantmentConfig.calculateCost(player.getTags(), vanillaCost, menu.costs[enchantmentOptionIndex]);
    }
}
