
# Tag-based Skills

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green)
![NeoForge](https://img.shields.io/badge/Mod%20Loader-NeoForge-orange)

**A tag-based skill mod for Minecraft 1.21.1, designed with Pufferfish's Skills in mind.**

Tag-based Skills is a Minecraft NeoForge mod that adds a collection of skills controlled through vanilla Minecraft tags.

The goal is to make skill-related mechanics easy to customize using Minecraft's existing tag system.

---

## Features

- **Currently added skills:** — Different Enchantment Costs per Player, Plant growth acceleration by Sneaking or Sprinting nearby
- **Tag-based configuration** — Control skill-related behavior using vanilla Minecraft tags.
- **Customizable** — Change skill configurations to your own gameplay needs in the config files.

## Requirements

| Component | Version |
|---|---|
| Minecraft | 1.21.1 |
| Mod Loader | NeoForge |
| NeoForge development version | 21.1.248 |

## Installation

1. Install Minecraft **1.21.1** with NeoForge.
2. Download the mod JAR from the [GitHub Releases](https://github.com/Meisterbauer04/Tag-based-Skills-/releases) page.
3. Place the downloaded `.jar` file into your Minecraft `mods` folder.
4. Launch Minecraft with the NeoForge profile.

## Configuration

Tag-based Skills uses Minecraft tags to control skill-related mechanics.

### Supported Tags

| Crop Growth Tags | Effect |
|---|---|
| skill_twerk_growth | Unlocks the ability to increase Crop grow speed by sneaking nearby |
| skill_twerk_radius_2=1 | Increases the Chance and max affected Blocks by 50% |
| skill_twerk_radius_3=2 | Increases the Chance and max affected Blocks by 100% |
| skill_twerk_power_2=1.5 | NeoForge |
| skill_twerk_power_3=2.0 | 21.1.248 |
| skill_sprint_growth |  Unlocks the ability to increase Crop grow speed by sprinting nearby |
| skill_sprint_radius_2=1 | Increase the Radius by one Block |
| skill_sprint_radius_3=2 | Increases the Radius by two Blocks |
| skill_sprint_power_2=1.5 | Increases the Chance and max affected Blocks by 50% |
| skill_sprint_power_3=2.0 | Increases the Chance and max affected Blocks by 100% |

*Note: Tags that modify the same Stat dont stack*

| Enchantment Tags | Effect |
|---|---|
| cheaper_enchantments_1 | Reduces Enchantment Cost by 25% |
| cheaper_enchantments_2 | Reduces Enchantment Cost by 50% |
| cheaper_enchantments_3 | Reduces Enchantment Cost by 75% |
| cheaper_enchantments_4 | Enchantment Cost matches Vanilla Minecraft 1.8+ |

*Note: This Mod changes Enchantment Cost behavior, as it was prior to Minecraft 1.8*



### Editing Tags

You can edit Tags in a dedicated config file for each skill under `config\TagBasedSkills`. There you can rename or add more tags. It is also possible to change the power behavior of each tag.

## Building from Source

To build the mod yourself, clone this repository and use the included Gradle wrapper.

**Windows (PowerShell):**

```powershell
.\gradlew.bat build
```

**Linux / macOS:**

```bash
./gradlew build
```

The compiled JAR can be found inside `build/libs/` after a successful build.

## Issues & Feedback

Found a bug or have a suggestion?

Please open an [issue on GitHub](https://github.com/Meisterbauer04/Tag-based-Skills-/issues).

When reporting a bug, include your Minecraft version, NeoForge version, steps to reproduce the issue, and any relevant logs.

## License

This project declares the MIT license. See the project's license file.

## Credits

- **Meisterbauer04** — Mod development
- **[NeoForged](https://github.com/neoforged/NeoForge)** — NeoForge modding framework
- **[Pufferfish's Skills](https://github.com/pufmat/skillsmod)** — Inspiration and skill-system integration
