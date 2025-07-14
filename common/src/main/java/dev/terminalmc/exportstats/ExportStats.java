/*
 * Copyright 2025 TerminalMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.terminalmc.exportstats;

import dev.terminalmc.exportstats.mixin.accessor.GeneralStatisticsListEntryAccessor;
import dev.terminalmc.exportstats.mixin.accessor.ItemStatisticsListAccessor;
import dev.terminalmc.exportstats.mixin.accessor.MobsStatisticsListMobRowAccessor;
import dev.terminalmc.exportstats.mixin.accessor.StatsScreenAccessor;
import dev.terminalmc.exportstats.platform.Services;
import dev.terminalmc.exportstats.util.ModLogger;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.achievement.StatsScreen.GeneralStatisticsList;
import net.minecraft.client.gui.screens.achievement.StatsScreen.ItemStatisticsList;
import net.minecraft.client.gui.screens.achievement.StatsScreen.ItemStatisticsList.ItemRow;
import net.minecraft.client.gui.screens.achievement.StatsScreen.MobsStatisticsList;
import net.minecraft.client.gui.screens.achievement.StatsScreen.MobsStatisticsList.MobRow;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class ExportStats {

    public static final String MOD_ID = "exportstats";
    public static final String MOD_NAME = "ExportStats";
    public static final ModLogger LOG = new ModLogger(MOD_NAME);
    public static final WidgetSprites EXPORT_SPRITES = new WidgetSprites(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "widget/export_button"),
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "widget/export_button_disabled"),
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "widget/export_button_highlighted")
    );

    public static final Path ROOT_PATH = Services.PLATFORM.getConfigDir().resolve(MOD_ID);
    public static final Component NO_VALUE_DISPLAY =
            StatsScreenAccessor.exportstats$getNoValueDisplay();

    public static @Nullable String lastWorld = null;
    public static boolean lastWorldIsServer = false;

    /**
     * Client initialization.
     */
    public static void init() {
    }

    public static void saveStats(
            StatsCounter counter,
            @Nullable GeneralStatisticsList generalStats,
            @Nullable ItemStatisticsList itemStats,
            @Nullable MobsStatisticsList mobStats,
            boolean open
    ) {
        @Nullable Path exportPath = null;
        if (generalStats != null)
            exportPath = saveStats(counter, generalStats, false);
        if (itemStats != null)
            exportPath = saveStats(counter, itemStats, false);
        if (mobStats != null)
            exportPath = saveStats(counter, mobStats, false);
        if (exportPath != null && open)
            Util.getPlatform().openPath(exportPath);
    }

    /**
     * Saves the general statistics data to a file.
     */
    public static Path saveStats(StatsCounter counter, GeneralStatisticsList stats, boolean open) {
        StringBuilder builder = new StringBuilder();

        for (GeneralStatisticsList.Entry row : stats.children()) {
            Stat<ResourceLocation> stat =
                    ((GeneralStatisticsListEntryAccessor) row).exportstats$getStat();
            String key = "stat." + stat.getValue().toString().replace(':', '.');
            String name = Component.translatable(key).getString();
            int value = counter.getValue(stat);
            builder.append("%s: %s\n".formatted(name, stat.format(value)));
        }

        String name = Component.translatable("stat.generalButton").getString();
        return save(name, builder.toString(), open);
    }

    /**
     * Saves the item statistics data to a file.
     */
    public static Path saveStats(StatsCounter counter, ItemStatisticsList stats, boolean open) {
        StringBuilder builder = new StringBuilder();

        for (ItemRow row : stats.children()) {
            String name = row.getItem().getDefaultInstance().getHoverName().getString();
            builder.append("%s\n".formatted(name));

            List<StatType<Block>> blockColumns =
                    ((ItemStatisticsListAccessor) stats).exportstats$getBlockColumns();
            for (StatType<Block> blockColumn : blockColumns) {
                Stat<Block> blockStat = row.getItem() instanceof BlockItem blockItem
                        ? blockColumn.get(blockItem.getBlock())
                        : null;

                Component component = blockStat == null
                        ? NO_VALUE_DISPLAY
                        : Component.literal(blockStat.format(counter.getValue(blockStat)));
                builder.append(String.format(
                        "    %s: %s\n",
                        blockColumn.getDisplayName().getString(),
                        component.getString()
                ));
            }

            List<StatType<Item>> itemColumns =
                    ((ItemStatisticsListAccessor) stats).exportstats$getItemColumns();
            for (StatType<Item> itemColumn : itemColumns) {
                Stat<Item> itemStat = itemColumn.get(row.getItem());

                Component component =
                        Component.literal(itemStat.format(counter.getValue(itemStat)));
                builder.append(String.format(
                        "    %s: %s\n",
                        itemColumn.getDisplayName().getString(),
                        component.getString()
                ));
            }
        }

        String name = Component.translatable("stat.itemsButton").getString();
        return save(name, builder.toString(), open);
    }

    /**
     * Saves the mob statistics data to a file.
     */
    public static Path saveStats(StatsCounter counter, MobsStatisticsList stats, boolean open) {
        StringBuilder builder = new StringBuilder();

        for (MobRow row : stats.children()) {
            Component name = ((MobsStatisticsListMobRowAccessor) row).exportstats$getMobName();
            builder.append("%s\n".formatted(name.getString()));

            Component kills = ((MobsStatisticsListMobRowAccessor) row).exportstats$getKills();
            builder.append("    %s\n".formatted(kills.getString()));

            Component killedBy =
                    ((MobsStatisticsListMobRowAccessor) row).exportstats$getKilledBy();
            builder.append("    %s\n".formatted(killedBy.getString()));
        }

        String name = Component.translatable("stat.mobsButton").getString();
        return save(name, builder.toString(), open);
    }

    /**
     * Saves the content to a file named for the specified category, then opens the file location.
     */
    private static Path save(String category, String content, boolean open) {
        Path exportPath = ROOT_PATH.resolve(getLastWorldName());
        String fileName = getFileNameFormat().formatted(category);
        save(exportPath, fileName, content);
        if (open)
            Util.getPlatform().openPath(exportPath);
        return exportPath;
    }

    /**
     * Saves the content to the specified file.
     */
    private static void save(Path dirPath, String filename, String content) {
        try {
            if (!Files.isDirectory(dirPath))
                Files.createDirectories(dirPath);
            Path file = dirPath.resolve(filename);
            Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");
            try (
                    OutputStreamWriter writer = new OutputStreamWriter(
                            new FileOutputStream(tempFile.toFile()),
                            StandardCharsets.UTF_8
                    )
            ) {
                writer.write(content);
            } catch (IOException e) {
                throw new IOException(e);
            }
            Files.move(
                    tempFile,
                    file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            LOG.error("Unable to save stats", e);
        }
    }

    /**
     * @return the name of the most recently visited world or server, with a corresponding prefix.
     */
    private static String getLastWorldName() {
        String name = lastWorld != null && !lastWorld.isBlank() ? lastWorld : "unknown";
        return (lastWorldIsServer ? "server_%s" : "world_%s").formatted(name);
    }

    /**
     * @return the name of the player.
     */
    private static String getFileNameFormat() {
        String name = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.getGameProfile().getName()
                : "player";
        return "%s_%s_%%s.txt".formatted(Component.translatable("gui.stats").getString(), name);
    }
}
