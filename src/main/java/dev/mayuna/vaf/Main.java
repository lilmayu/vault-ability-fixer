package dev.mayuna.vaf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;

public class Main {

    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length != 4) {
            LOGGER.error("Invalid number of arguments. Expected 4, got {}", args.length);
            return;
        }

        String abilityFileName = args[0];
        String talentsFileName = args[1];
        String vaultLevelFileName = args[2];
        String questsFileName = args[3];

        LOGGER.info("=== Vault Ability Fixer @ 1.1 ===");
        LOGGER.info("Made by Mayuna");
        LOGGER.info("=================================");
        LOGGER.info("Ability file: {}", abilityFileName);
        LOGGER.info("Talents file: {}", talentsFileName);
        LOGGER.info("Vault Level file: {}", vaultLevelFileName);
        LOGGER.info("Quests file: {}", questsFileName);
        LOGGER.info("");

        if (!Files.exists(Path.of(abilityFileName))) {
            LOGGER.error("Ability file does not exist.");
            return;
        }

        if (!Files.exists(Path.of(talentsFileName))) {
            LOGGER.error("Talents file does not exist.");
            return;
        }

        if (!Files.exists(Path.of(vaultLevelFileName))) {
            LOGGER.error("Vault Level file does not exist.");
            return;
        }

        if (!Files.exists(Path.of(questsFileName))) {
            LOGGER.error("Quests file does not exist.");
            return;
        }

        LOGGER.info("Type 'yes' to analyze files.");

        waitForInputYes();

        LOGGER.info("Loading abilities file...");
        NamedTag abilityNamedTag = readNbtFile(abilityFileName);

        LOGGER.info("Loading talents file...");
        NamedTag talentsNamedTag = readNbtFile(talentsFileName);

        LOGGER.info("Loading vault level file...");
        NamedTag vaultLevelNamedTag = readNbtFile(vaultLevelFileName);

        LOGGER.info("Loading quests file...");
        NamedTag questsNamedTag = readNbtFile(questsFileName);

        CompoundTag abilityFile = (CompoundTag)abilityNamedTag.getTag();
        CompoundTag talentsFile = (CompoundTag)talentsNamedTag.getTag();
        CompoundTag vaultLevelFile = (CompoundTag)vaultLevelNamedTag.getTag();
        CompoundTag questsFile = (CompoundTag)questsNamedTag.getTag();
        List<VaultPlayer> vaultPlayers = VaultPlayer.loadPlayers(vaultLevelFile);

        for (VaultPlayer vaultPlayer : vaultPlayers) {
            vaultPlayer.loadLevelData(vaultLevelFile);
        }

        for (VaultPlayer vaultPlayer : vaultPlayers) {
            vaultPlayer.calculateSpentPoints(abilityFile, talentsFile);
        }

        for (VaultPlayer vaultPlayer : vaultPlayers) {
            vaultPlayer.deleteHunterAbility(abilityFile);
            vaultPlayer.deleteSpeedTalent(talentsFile);
        }

        for (VaultPlayer vaultPlayer : vaultPlayers) {
            vaultPlayer.giveAbilityPoints(vaultLevelFile, questsFile);
        }

        LOGGER.info("");
        LOGGER.info("");
        LOGGER.info("");
        LOGGER.info("Data fixing and refunding skill points DONE.");
        LOGGER.info("Type 'yes' to save the files.");

        waitForInputYes();

        try {
            NBTUtil.write(abilityNamedTag, abilityFileName);
            NBTUtil.write(talentsNamedTag, talentsFileName);
            NBTUtil.write(vaultLevelNamedTag, vaultLevelFileName);
        } catch (Exception e) {
            LOGGER.error("An error occurred while saving the NBT files.");
            throw new RuntimeException(e);
        }

        LOGGER.info("Saved.");
    }

    private static NamedTag readNbtFile(String filePath) {
        try {
            return NBTUtil.read(filePath);
        } catch (Exception e) {
            LOGGER.error("An error occurred while reading the NBT file.");
            throw new RuntimeException(e);
        }
    }

    private static void waitForInputYes() {
        try {
            Scanner scanner = new Scanner(System.in);
            String line;

            // Check if user typed "yes"
            while ((line = scanner.nextLine()) != null) {
                if (line.equalsIgnoreCase("yes")) {
                    break;
                }
            }

        } catch (Exception e) {
            LOGGER.error("An error occurred while waiting for input.");
            throw new RuntimeException(e);
        }
    }
}
