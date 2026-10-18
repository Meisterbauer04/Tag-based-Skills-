package com.meisterbauer.tagbasedskills.config;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * Loads and evaluates player-tag tiers that reduce enchanting-table level costs.
 * The loaded snapshot is immutable so gameplay reads see a consistent configuration.
 */
public final class EnchantmentConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CONFIG_FILE_NAME = "enchantments.toml";
    private static final String ENABLED_KEY = "enabled";
    private static final String TAG_KEY_SUFFIX = "_tag";
    private static final String DISCOUNT_KEY_SUFFIX = "_discount";
    private static final String LEVEL_KEY_PREFIX = "level_";
    private static final double MINIMUM_DISCOUNT = 0.0D;
    private static final double MAXIMUM_DISCOUNT = 1.0D;

    private static final EnchantmentConfigSnapshot DEFAULTS = new EnchantmentConfigSnapshot(
            true,
            List.of(
                    new EnchantmentTier(1, "cheaper_enchantments_1", 0.25D),
                    new EnchantmentTier(2, "cheaper_enchantments_2", 0.50D),
                    new EnchantmentTier(3, "cheaper_enchantments_3", 0.75D),
                    new EnchantmentTier(4, "cheaper_enchantments_4", 1.00D)
            )
    );

    private static volatile EnchantmentConfigSnapshot current = DEFAULTS;

    private EnchantmentConfig() {
    }

    // Creates the config file when absent, then atomically replaces the active settings snapshot.
    public static void initialize() {
        Path path = ConfigPaths.resolve(CONFIG_FILE_NAME);
        try {
            ConfigPaths.createIfAbsent(path, defaultConfigText());
            current = readSnapshot(TomlConfig.read(path));
            LOGGER.info("Loaded enchantment config from {}", path);
        } catch (IOException exception) {
            LOGGER.error("Failed to load enchantment config {}; using defaults.", path, exception);
            current = DEFAULTS;
        }
    }

    /*
     * Calculates the charged level cost. A matching tier moves the table requirement toward the
     * vanilla one-, two-, or three-level charge; the highest matching discount takes precedence.
     */
    public static int calculateCost(Set<String> playerTags, int vanillaCost, int requirement) {
        if (!current.enabled() || requirement <= 0) {
            return vanillaCost;
        }

        double discount = current.discountFor(playerTags);
        double scaledCost = vanillaCost + (requirement - vanillaCost) * (MAXIMUM_DISCOUNT - discount);
        return Math.max(vanillaCost, (int) Math.round(scaledCost));
    }

    private static EnchantmentConfigSnapshot readSnapshot(Map<String, String> values) {
        boolean enabled = TomlConfig.booleanValue(values, ENABLED_KEY, DEFAULTS.enabled());
        List<EnchantmentTier> tiers = DEFAULTS.tiers().stream()
                .map(defaultTier -> readTier(values, defaultTier))
                .toList();
        return new EnchantmentConfigSnapshot(enabled, tiers);
    }

    private static EnchantmentTier readTier(Map<String, String> values, EnchantmentTier defaults) {
        String keyPrefix = LEVEL_KEY_PREFIX + defaults.level();
        String tag = TomlConfig.stringValue(values, keyPrefix + TAG_KEY_SUFFIX, defaults.tag());
        double discount = clamp(
                TomlConfig.doubleValue(values, keyPrefix + DISCOUNT_KEY_SUFFIX, defaults.discount()),
                MINIMUM_DISCOUNT,
                MAXIMUM_DISCOUNT
        );
        return new EnchantmentTier(defaults.level(), tag, discount);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    // Produces the initial on-disk configuration, including user-facing guidance.
    private static String defaultConfigText() {
        return """
                # Enchantment XP Cost Configuration
                # A player without a skill pays the table requirement. Each tier moves the cost towards vanilla.
                # 0.00 = requirement cost; 1.00 = normal vanilla 1/2/3-level cost.
                # Changes take effect after restarting the game or server.

                enabled = true

                level_1_tag = "cheaper_enchantments_1"
                level_2_tag = "cheaper_enchantments_2"
                level_3_tag = "cheaper_enchantments_3"
                level_4_tag = "cheaper_enchantments_4"

                level_1_discount = 0.25
                level_2_discount = 0.50
                level_3_discount = 0.75
                level_4_discount = 1.00
                """;
    }

    private record EnchantmentTier(int level, String tag, double discount) {
    }

    private record EnchantmentConfigSnapshot(boolean enabled, List<EnchantmentTier> tiers) {
        private double discountFor(Set<String> playerTags) {
            return tiers.stream()
                    .filter(tier -> playerTags.contains(tier.tag()))
                    .mapToDouble(EnchantmentTier::discount)
                    .max()
                    .orElse(MINIMUM_DISCOUNT);
        }
    }
}
