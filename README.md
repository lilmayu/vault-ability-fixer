# Vault Ability Fixer

I've decided to write small utility to fix broken hunter and speed ability.

This does:
- Fixes broken hunter ability, players not able to open skills tab
- Fixes speed talent, some players can have speed 4 instead of cap 2
- Refunds the points spent on the broken abilities/talents

## Usage
1. Download JAR in the downloads sections (or compile from source, `gradlew shadowJar`)
2. Stop the server
3. Download four files from the server and place them in the same directory as the JAR
    - `the_vault_PlayerAbilities.dat`
    - `the_vault_PlayerTalents.dat`
    - `the_vault_PlayerVaultLevels.dat`
    - `the_vault_QuestStates.dat`
4. **MAKE A BACKUP OF THOSE FILES**
5. Open CMD/Terminal in the directory
6. Run `java -jar vault-ability-fixer-1.1-all.jar the_vault_PlayerAbilities.dat the_vault_PlayerTalents.dat the_vault_PlayerVaultLevels.dat the_vault_QuestStates.dat`
    - Note the order of the files, first abilities, second talents, third vault levels, fourth quest states
    - **You will need at least Java 17**
7. It will check if it can find the files
8. Type 'yes'
9. It will analyze and fix the files IN MEMORY (not yet written to the filesystem)
10. Read the log and see if everything is correct
11. Type 'yes' to write the changes to the files
12. Copy the three files back to the server
13. Start the server

## Testing

Me and Dulla tested this on singleplayer worlds and multiplayer as well. Works without any problems, refunds correct amount of points and fixes the abilities.