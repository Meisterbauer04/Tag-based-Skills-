package com.meisterbauer.tagbasedskills.config;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * Loads tag-based crop-growth skill settings and exposes the effective upgrade values.
 * For a given upgrade kind, the highest matching player-tag value wins rather than stacking.
 */
public final class CropGrowthConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CONFIG_FILE_NAME = "crop_growth.toml";
    private static final String UNLOCK_TAG_KEY = "unlock_tag";
    private static final String RADIUS_UPGRADES_KEY = "radius_upgrades";
    private static final String POWER_UPGRADES_KEY = "power_upgrades";
    private static final String POWER_BEHAVIOR_SECTION = "power_behavior.";
    private static final String AFFECTS_CHANCE_KEY = POWER_BEHAVIOR_SECTION + "affects_chance";
    private static final String AFFECTS_MAX_BLOCKS_KEY = POWER_BEHAVIOR_SECTION + "affects_max_blocks";
    private static final String CHANCE_CAP_KEY = POWER_BEHAVIOR_SECTION + "chance_cap";
    private static final char UPGRADE_SEPARATOR = '=';
    // Bounds applied to player-editable configuration values.
    private static final int MINIMUM_RADIUS = 0;
    private static final int MINIMUM_AFFECTED_BLOCKS = 1;
    private static final double MINIMUM_CHANCE = 0.0D;
    private static final double MAXIMUM_CHANCE = 1.0D;
    private static final double DEFAULT_POWER_MULTIPLIER = 1.0D;

    private static final ConfigSnapshot DEFAULTS = new ConfigSnapshot(
            new GrowthSkillSettings(
                    "skill_twerk_growth",
                    List.of(new RadiusUpgrade("skill_twerk_radius_2", 1), new RadiusUpgrade("skill_twerk_radius_3", 2)),
                    List.of(new PowerUpgrade("skill_twerk_power_2", 1.5D), new PowerUpgrade("skill_twerk_power_3", 2.0D))
            ),
            new GrowthSkillSettings(
                    "skill_sprint_growth",
                    List.of(new RadiusUpgrade("skill_sprint_radius_2", 1), new RadiusUpgrade("skill_sprint_radius_3", 2)),
                    List.of(new PowerUpgrade("skill_sprint_power_2", 1.5D), new PowerUpgrade("skill_sprint_power_3", 2.0D))
            ),
            true,
            true,
            MAXIMUM_CHANCE
    );

    private static volatile ConfigSnapshot current = DEFAULTS;

    private CropGrowthConfig() {
    }

    // Creates the config file when absent, then replaces the active immutable settings snapshot.
    public static void initialize() {
        Path path = ConfigPaths.resolve(CONFIG_FILE_NAME);
        try {
            ConfigPaths.createIfAbsent(path, defaultConfigText());
            current = readSnapshot(TomlConfig.read(path));
            LOGGER.info("Loaded crop growth config from {}", path);
        } catch (IOException exception) {
            LOGGER.error("Failed to load crop growth config {}; using defaults.", path, exception);
            current = DEFAULTS;
        }
    }

    // Returns whether the player has the base tag required to activate this growth skill.
    public static boolean isUnlocked(Set<String> playerTags, GrowthType growthType) {
        return playerTags.contains(settingsFor(growthType).unlockTag());
    }

    // Returns the largest configured radius bonus among the player's matching upgrade tags.
    public static int radiusBonus(Set<String> playerTags, GrowthType growthType) {
        return settingsFor(growthType).radiusUpgrades().stream()
                .filter(upgrade -> playerTags.contains(upgrade.tag()))
                .mapToInt(RadiusUpgrade::bonus)
                .max()
                .orElse(MINIMUM_RADIUS);
    }

    // Returns the largest configured power multiplier among the player's matching upgrade tags.
    public static double powerMultiplier(Set<String> playerTags, GrowthType growthType) {
        return settingsFor(growthType).powerUpgrades().stream()
                .filter(upgrade -> playerTags.contains(upgrade.tag()))
                .mapToDouble(PowerUpgrade::multiplier)
                .max()
                .orElse(DEFAULT_POWER_MULTIPLIER);
    }

    // Applies the max-block behavior flag and ensures a positive number of blocks can be affected.
    public static int upgradedMaxBlocks(int maxBlocks, double multiplier) {
        if (!current.powerAffectsMaxBlocks()) {
            return maxBlocks;
        }
        return Math.max(MINIMUM_AFFECTED_BLOCKS, (int) Math.ceil(maxBlocks * multiplier));
    }

    // Applies the chance behavior flag and caps the result to the configured safe range.
    public static double upgradedChance(double chance, double multiplier) {
        if (!current.powerAffectsChance()) {
            return chance;
        }
        return clamp(chance * multiplier, MINIMUM_CHANCE, current.chanceCap());
    }

    private static GrowthSkillSettings settingsFor(GrowthType growthType) {
        return growthType == GrowthType.TWERK ? current.twerk() : current.sprint();
    }

    private static ConfigSnapshot readSnapshot(Map<String, String> values) {
        return new ConfigSnapshot(
                readGrowthSettings(values, GrowthType.TWERK, DEFAULTS.twerk()),
                readGrowthSettings(values, GrowthType.SPRINT, DEFAULTS.sprint()),
                TomlConfig.booleanValue(values, AFFECTS_CHANCE_KEY, DEFAULTS.powerAffectsChance()),
                TomlConfig.booleanValue(values, AFFECTS_MAX_BLOCKS_KEY, DEFAULTS.powerAffectsMaxBlocks()),
                clamp(TomlConfig.doubleValue(values, CHANCE_CAP_KEY, DEFAULTS.chanceCap()), MINIMUM_CHANCE, MAXIMUM_CHANCE)
        );
    }

    private static GrowthSkillSettings readGrowthSettings(Map<String, String> values, GrowthType type, GrowthSkillSettings defaults) {
        String section = type.configSection() + '.';
        return new GrowthSkillSettings(
                TomlConfig.stringValue(values, section + UNLOCK_TAG_KEY, defaults.unlockTag()),
                readRadiusUpgrades(values, section + RADIUS_UPGRADES_KEY, defaults.radiusUpgrades()),
                readPowerUpgrades(values, section + POWER_UPGRADES_KEY, defaults.powerUpgrades())
        );
    }

    private static List<RadiusUpgrade> readRadiusUpgrades(Map<String, String> values, String key, List<RadiusUpgrade> fallback) {
        if (!values.containsKey(key)) {
            return fallback;
        }
        List<String> entries = TomlConfig.stringArray(values, key);
        List<RadiusUpgrade> upgrades = new ArrayList<>();
        for (String entry : entries) {
            ParsedUpgrade parsed = parseUpgrade(entry);
            if (parsed == null) {
                warnInvalidUpgrade(entry);
                continue;
            }
            try {
                int bonus = Integer.parseInt(parsed.value());
                if (bonus >= MINIMUM_RADIUS) {
                    upgrades.add(new RadiusUpgrade(parsed.tag(), bonus));
                    continue;
                }
            } catch (NumberFormatException ignored) {
            }
            warnInvalidUpgrade(entry);
        }
        return List.copyOf(upgrades);
    }

    private static List<PowerUpgrade> readPowerUpgrades(Map<String, String> values, String key, List<PowerUpgrade> fallback) {
        if (!values.containsKey(key)) {
            return fallback;
        }
        List<String> entries = TomlConfig.stringArray(values, key);
        List<PowerUpgrade> upgrades = new ArrayList<>();
        for (String entry : entries) {
            ParsedUpgrade parsed = parseUpgrade(entry);
            if (parsed == null) {
                warnInvalidUpgrade(entry);
                continue;
            }
            try {
                double multiplier = Double.parseDouble(parsed.value());
                if (Double.isFinite(multiplier) && multiplier >= MINIMUM_CHANCE) {
                    upgrades.add(new PowerUpgrade(parsed.tag(), multiplier));
                    continue;
                }
            } catch (NumberFormatException ignored) {
            }
            warnInvalidUpgrade(entry);
        }
        return List.copyOf(upgrades);
    }

    private static ParsedUpgrade parseUpgrade(String entry) {
        int separatorIndex = entry.indexOf(UPGRADE_SEPARATOR);
        if (separatorIndex <= 0 || separatorIndex >= entry.length() - 1) {
            return null;
        }
        String tag = entry.substring(0, separatorIndex).trim();
        String value = entry.substring(separatorIndex + 1).trim();
        return tag.isEmpty() || value.isEmpty() ? null : new ParsedUpgrade(tag, value);
    }

    private static void warnInvalidUpgrade(String entry) {
        LOGGER.warn("Ignoring invalid crop growth upgrade entry: {}", entry);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    // Produces the initial on-disk configuration, including supported upgrade syntax.
    private static String defaultConfigText() {
        return """
                # Tag Based Skills: native crop growth skills
                # Upgrade format: "player_tag=value". Matching upgrades do not stack; the highest value wins.
                # Changes take effect after restarting the game or server.

                [twerk]
                unlock_tag = "skill_twerk_growth"
                radius_upgrades = [
                    "skill_twerk_radius_2=1",
                    "skill_twerk_radius_3=2"
                ]
                power_upgrades = [
                    "skill_twerk_power_2=1.5",
                    "skill_twerk_power_3=2.0"
                ]

                [sprint]
                unlock_tag = "skill_sprint_growth"
                radius_upgrades = [
                    "skill_sprint_radius_2=1",
                    "skill_sprint_radius_3=2"
                ]
                power_upgrades = [
                    "skill_sprint_power_2=1.5",
                    "skill_sprint_power_3=2.0"
                ]

                [power_behavior]
                affects_chance = true
                affects_max_blocks = true
                chance_cap = 1.0
                """;
    }

    public enum GrowthType {
        TWERK("twerk"),
        SPRINT("sprint");

        private final String configSection;

        GrowthType(String configSection) {
            this.configSection = configSection;
        }

        private String configSection() {
            return configSection;
        }
    }

    private record RadiusUpgrade(String tag, int bonus) {
    }

    private record PowerUpgrade(String tag, double multiplier) {
    }

    private record ParsedUpgrade(String tag, String value) {
    }

    private record GrowthSkillSettings(String unlockTag, List<RadiusUpgrade> radiusUpgrades, List<PowerUpgrade> powerUpgrades) {
    }

    private record ConfigSnapshot(
            GrowthSkillSettings twerk,
            GrowthSkillSettings sprint,
            boolean powerAffectsChance,
            boolean powerAffectsMaxBlocks,
            double chanceCap
    ) {
    }
}
