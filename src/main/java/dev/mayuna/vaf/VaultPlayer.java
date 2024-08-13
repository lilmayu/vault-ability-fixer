package dev.mayuna.vaf;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Data;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.StringTag;

@Data
public class VaultPlayer {

    private static final Logger LOGGER = LogManager.getLogger(VaultPlayer.class);

    private String playerUuid;
    private int vaultLevel;
    private int unspentSkillPoints;
    private int spentSkillPointsWithoutHunter;

    private int abilityFileIndex = -1;
    private int talentFileIndex = -1;
    private int levelsFileIndex = -1;

    public VaultPlayer(String playerUuid) {
        this.playerUuid = playerUuid;
    }

    public static List<VaultPlayer> loadPlayers(CompoundTag levelsFile) {
        LOGGER.info("Loading players from levels file...");
        CompoundTag data = levelsFile.getCompoundTag("data");
        ListTag<StringTag> playerEntries = data.getListTag("PlayerEntries").asStringTagList();
        List<VaultPlayer> players = new LinkedList<>();

        for (int i = 0; i < playerEntries.size(); i++) {
            StringTag playerUuid = playerEntries.get(i);
            LOGGER.info("- Found player uuid: {}", playerUuid.getValue());
            players.add(new VaultPlayer(playerUuid.getValue()));
        }

        LOGGER.info("Loaded {} players", players.size());
        return players;
    }

    public boolean loadLevelData(CompoundTag levelsFile) {
        LOGGER.info("> Loading level data for player {}", playerUuid);
        CompoundTag data = levelsFile.getCompoundTag("data");
        ListTag<StringTag> playerEntries = data.getListTag("PlayerEntries").asStringTagList();
        ListTag<CompoundTag> statEntries = data.getListTag("StatEntries").asCompoundTagList();

        for (int i = 0; i < playerEntries.size(); i++) {
            StringTag playerEntry = playerEntries.get(i);
            if (playerEntry.getValue().equals(playerUuid)) {
                levelsFileIndex = i;
                LOGGER.info("- Found player level entry at index {}", i);
                break;
            }
        }

        if (levelsFileIndex == -1) {
            LOGGER.warn("Player {} is not in the levels file", playerUuid);
            return false;
        }

        CompoundTag statEntry = statEntries.get(levelsFileIndex);
        vaultLevel = statEntry.getInt("vaultLevel");
        unspentSkillPoints = statEntry.getInt("unspentSkillPts");
        LOGGER.info("- Loaded vault level: {}", vaultLevel);
        LOGGER.info("- Loaded unspent skill points: {}", unspentSkillPoints);
        return true;
    }

    public void calculateSpentPoints(CompoundTag abilitiesFile, CompoundTag talentsFile) {
        LOGGER.info("> Calculating spent points for player {}", playerUuid);
        CompoundTag abilityData = abilitiesFile.getCompoundTag("data");
        CompoundTag talentData = talentsFile.getCompoundTag("data");
        ListTag<StringTag> abilityPlayers = abilityData.getListTag("Players").asStringTagList();
        ListTag<StringTag> talentPlayers = talentData.getListTag("Players").asStringTagList();
        ListTag<CompoundTag> abilities = abilityData.getListTag("Abilities").asCompoundTagList();
        ListTag<CompoundTag> talents = talentData.getListTag("Talents").asCompoundTagList();

        for (int i = 0; i < abilityPlayers.size(); i++) {
            StringTag player = abilityPlayers.get(i);
            if (player.getValue().equals(playerUuid)) {
                abilityFileIndex = i;
                LOGGER.info("- Found player ability entry at index {}", i);
                break;
            }
        }

        if (abilityFileIndex == -1) {
            LOGGER.warn("Player {} is not in the ability file", playerUuid);
            return;
        }

        for (int i = 0; i < talentPlayers.size(); i++) {
            StringTag player = talentPlayers.get(i);
            if (player.getValue().equals(playerUuid)) {
                talentFileIndex = i;
                LOGGER.info("- Found player talent entry at index {}", i);
                break;
            }
        }

        if (talentFileIndex == -1) {
            LOGGER.warn("Player {} is not in the talent file", playerUuid);
            return;
        }

        LOGGER.info("- Reading player's abilities...");

        CompoundTag playerAbilities = abilities.get(abilityFileIndex);
        ListTag<CompoundTag> skills = playerAbilities.getListTag("skills").asCompoundTagList();

        int totalSpentPoints = 0;

        for (int i = 0; i < skills.size(); i++) {
            CompoundTag skill = skills.get(i);
            String id = skill.getString("id");

            LOGGER.info("-- Reading skill: {}", id);

            if (id.equals("Hunter")) {
                LOGGER.info("--- Ignoring hunter skill...");
                continue;
            }

            byte index = skill.getByte("index");
            ListTag<CompoundTag> specializations = skill.getListTag("specializations").asCompoundTagList();

            if (specializations.size() <= index) {
                LOGGER.warn("--- Specializations size ({}) is less than index ({})!", specializations.size(), index);
                continue;
            }

            CompoundTag specialization = specializations.get(index);

            totalSpentPoints += countCostsFromTierTag(specialization);
        }

        LOGGER.info("- Reading player's talents...");

        CompoundTag playerTalents = talents.get(talentFileIndex);
        ListTag<CompoundTag> talentSkills = playerTalents.getListTag("skills").asCompoundTagList();

        for (int i = 0; i < talentSkills.size(); i++) {
            CompoundTag talentSkill = talentSkills.get(i);
            String id = talentSkill.getString("id");
            String type = talentSkill.getString("type");

            LOGGER.info("-- Reading talent {} of type {}", id, type);

            if (id.equals("Speed")) {
                LOGGER.info("--- Ignoring speed talent...");
                continue;
            }

            if (type.equals("grouped")) {
                ListTag<CompoundTag> children = talentSkill.getListTag("children").asCompoundTagList();

                for (int j = 0; j < children.size(); j++) {
                    CompoundTag child = children.get(j);
                    totalSpentPoints += countCostsFromTierTag(child);
                }
            } else {
                totalSpentPoints += countCostsFromTierTag(talentSkill);
            }
        }

        LOGGER.info("- Total spent points: {}", totalSpentPoints);
        spentSkillPointsWithoutHunter = totalSpentPoints;
    }

    private int countCostsFromTierTag(CompoundTag rootTierTag) {
        byte tier = rootTierTag.getByte("tier");

        if (tier == 0) {
            LOGGER.info("--- Player not learned this skill/talent");
            return 0;
        }

        ListTag<CompoundTag> tiers = rootTierTag.getListTag("tiers").asCompoundTagList();

        if (tiers.size() < tier) {
            LOGGER.warn("--- Tiers size ({}) is less than tier ({})!", tiers.size(), tier);
            return 0;
        }

        int totalLearnCostForSkill = 0;

        for (int j = 0; j < tier; j++) {
            CompoundTag tierTag = tiers.get(j);
            String tierId = tierTag.getString("id");
            byte learnPointCost = tierTag.getByte("learnPointCost");
            totalLearnCostForSkill += learnPointCost;

            LOGGER.info("---- Tier index {} (id {}): costs {} points", j, tierId, learnPointCost);
        }

        LOGGER.info("--- Total learn cost: {}", totalLearnCostForSkill);

        return totalLearnCostForSkill;
    }

    public void deleteHunterAbility(CompoundTag abilitiesFile) {
        LOGGER.info("> Fixing hunter ability for player {}", playerUuid);

        if (abilityFileIndex == -1 || talentFileIndex == -1) {
            LOGGER.warn("- Indexes not loaded for this player, cannot fix hunter ability.");
            return;
        }

        CompoundTag abilityData = abilitiesFile.getCompoundTag("data");
        ListTag<CompoundTag> abilities = abilityData.getListTag("Abilities").asCompoundTagList();

        LOGGER.info("- Reading player's abilities...");

        if (abilityFileIndex == -1) {
            LOGGER.error("This should not happen.");
            return;
        }

        CompoundTag playerAbilities = abilities.get(abilityFileIndex);
        ListTag<CompoundTag> skills = playerAbilities.getListTag("skills").asCompoundTagList();

        int hunterIndex = -1;

        for (int i = 0; i < skills.size(); i++) {
            CompoundTag skill = skills.get(i);
            String id = skill.getString("id");

            LOGGER.info("-- Reading skill: {}", id);

            if (id.equals("Hunter")) {
                LOGGER.info("--- Found hunter skill at index {}", i);
                hunterIndex = i;
                break;
            }
        }

        if (hunterIndex == -1) {
            LOGGER.info("--- Hunter skill not found, nothing to do.");
            return;
        }

        LOGGER.info("-- Fixing hunter skill...");
        skills.remove(hunterIndex);
    }

    public void deleteSpeedTalent(CompoundTag talentsFile) {
        LOGGER.info("> Fixing speed talent for player {}", playerUuid);

        if (abilityFileIndex == -1 || talentFileIndex == -1) {
            LOGGER.warn("- Indexes not loaded for this player, cannot fix speed talent.");
            return;
        }

        CompoundTag talentData = talentsFile.getCompoundTag("data");
        ListTag<CompoundTag> talents = talentData.getListTag("Talents").asCompoundTagList();

        LOGGER.info("- Reading player's talents...");

        CompoundTag playerTalents = talents.get(talentFileIndex);
        ListTag<CompoundTag> talentSkills = playerTalents.getListTag("skills").asCompoundTagList();

        int speedIndex = -1;

        for (int i = 0; i < talentSkills.size(); i++) {
            CompoundTag talentSkill = talentSkills.get(i);
            String id = talentSkill.getString("id");

            LOGGER.info("-- Reading talent: {}", id);

            if (id.equals("Speed")) {
                LOGGER.info("--- Found speed talent at index {}", i);
                speedIndex = i;
                break;
            }
        }

        if (speedIndex == -1) {
            LOGGER.info("--- Speed talent not found, nothing to do.");
            return;
        }

        LOGGER.info("-- Fixing speed talent...");
        talentSkills.remove(speedIndex);
    }

    public void giveAbilityPoints(CompoundTag levelsFile, CompoundTag questsFile) {
        if (abilityFileIndex == -1 || talentFileIndex == -1) {
            LOGGER.warn("- Indexes not loaded for player {}, cannot give ability points.", playerUuid);
            return;
        }

        LOGGER.info("- Checking if player has completed learning_skills quest...");
        int extraSkillPoint = 0;

        CompoundTag questData = questsFile.getCompoundTag("data");
        CompoundTag playerQuests = questData.getCompoundTag(playerUuid);

        if (playerQuests != null) {
            ListTag<StringTag> completedQuests = playerQuests.getListTag("Completed").asStringTagList();

            for (int i = 0; i < completedQuests.size(); i++) {
                StringTag quest = completedQuests.get(i);

                if (quest.getValue().equals("learning_skills")) {
                    LOGGER.info("-- Player has completed learning_skills quest, adding them 1 skill point.");
                    extraSkillPoint = 1;
                    break;
                }
            }
        } else {
            LOGGER.warn("-- Player has no quests data, cannot check if they have completed learning_skills quest.");
        }

        int pointsToGive = vaultLevel - spentSkillPointsWithoutHunter - unspentSkillPoints + extraSkillPoint;

        if (pointsToGive < 0) {
            LOGGER.warn("- Player {} has more spent points than their vault level, nothing to do.", playerUuid);
            return;
        }

        int newUnspentSkillPoints = unspentSkillPoints + pointsToGive;

        if (newUnspentSkillPoints < 0) {
            LOGGER.warn("- Player {} has negative unspent skill points!", playerUuid);
            return;
        }

        LOGGER.info("> Giving {} ability points to player {} - they will have {} points", pointsToGive, playerUuid, newUnspentSkillPoints);

        if (levelsFileIndex == -1) {
            LOGGER.error("This should not happen.");
            return;
        }

        CompoundTag data = levelsFile.getCompoundTag("data");
        ListTag<CompoundTag> statEntries = data.getListTag("StatEntries").asCompoundTagList();
        CompoundTag statEntry = statEntries.get(levelsFileIndex);

        statEntry.putInt("unspentSkillPts", newUnspentSkillPoints);
    }
}
