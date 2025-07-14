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

package dev.terminalmc.exportstats.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.terminalmc.exportstats.ExportStats;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.StatsCounter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import static dev.terminalmc.exportstats.util.Localization.localized;

@Mixin(StatsScreen.class)
public abstract class StatsScreenMixin extends Screen {

    @Shadow
    @Nullable
    private StatsScreen.GeneralStatisticsList statsList;

    @Shadow
    @Nullable StatsScreen.ItemStatisticsList itemStatsList;

    @Shadow
    @Nullable
    private StatsScreen.MobsStatisticsList mobsStatsList;

    @Shadow
    @Final
    StatsCounter stats;

    public StatsScreenMixin(Component text) {
        super(text);
    }

    @WrapOperation(
            method = "initButtons",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/achievement/StatsScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;"
            )
    )
    public GuiEventListener addExportButtons(
            StatsScreen instance,
            GuiEventListener child,
            Operation<GuiEventListener> original
    ) {
        int iconButtonSize = 20;
        int exportAllButtonWidth = 60;
        int padding = 5;

        GuiEventListener retVal;
        if (child instanceof Button button) {
            if (exportstats$buttonMatches(button, "stat.generalButton")) {
                button.setWidth(button.getWidth() - (iconButtonSize + padding));
                retVal = original.call(instance, button);
                Button exportButton = exportstats$createIconButton(
                        localized("button", "export.general.tooltip"),
                        (b) -> {
                            if (statsList != null && !statsList.children().isEmpty())
                                ExportStats.saveStats(stats, statsList, true);
                        },
                        statsList != null && !statsList.children().isEmpty()
                );
                exportButton.setX(button.getX() + button.getWidth());
                exportButton.setY(button.getY());
                addRenderableWidget(exportButton);

            } else if (exportstats$buttonMatches(button, "stat.itemsButton")) {
                button.setWidth(button.getWidth() - (iconButtonSize + padding));
                retVal = original.call(instance, button);
                Button exportButton = exportstats$createIconButton(
                        localized("button", "export.items.tooltip"),
                        (b) -> {
                            if (itemStatsList != null && !itemStatsList.children().isEmpty())
                                ExportStats.saveStats(stats, itemStatsList, true);
                        },
                        itemStatsList != null && !itemStatsList.children().isEmpty()
                );
                exportButton.setX(button.getX() + button.getWidth());
                exportButton.setY(button.getY());
                addRenderableWidget(exportButton);

            } else if (exportstats$buttonMatches(button, "stat.mobsButton")) {
                button.setWidth(button.getWidth() - (iconButtonSize + padding));
                retVal = original.call(instance, button);
                Button exportButton = exportstats$createIconButton(
                        localized("button", "export.mobs.tooltip"),
                        (b) -> {
                            if (mobsStatsList != null && !mobsStatsList.children().isEmpty())
                                ExportStats.saveStats(stats, mobsStatsList, true);
                        },
                        mobsStatsList != null && !mobsStatsList.children().isEmpty()
                );
                exportButton.setX(button.getX() + button.getWidth());
                exportButton.setY(button.getY());
                addRenderableWidget(exportButton);

            } else if (button.getMessage().equals(CommonComponents.GUI_DONE)) {
                button.setWidth(button.getWidth() - (exportAllButtonWidth + padding));
                Button exportAllButton = Button.builder(
                        localized("button", "export.all"),
                        (b) -> ExportStats.saveStats(
                                stats,
                                statsList,
                                itemStatsList,
                                mobsStatsList,
                                true
                        )
                ).width(exportAllButtonWidth).build();
                exportAllButton.setX(button.getX());
                exportAllButton.setY(button.getY());
                button.setX(button.getX() + exportAllButtonWidth + padding);
                addRenderableWidget(exportAllButton);
                original.call(instance, button);
                retVal = button;

            } else {
                retVal = original.call(instance, child);
            }
        } else {
            retVal = original.call(instance, child);
        }
        return retVal;
    }

    @Unique
    private boolean exportstats$buttonMatches(Button button, String key) {
        return button.getMessage().getString().equals(Component.translatable(key).getString());
    }

    @Unique
    private @NotNull Button exportstats$createIconButton(
            Component msg,
            OnPress onPress,
            boolean active
    ) {
        Button exportButton = new ImageButton(
                0,
                0,
                20,
                20,
                0,
                0,
                20,
                ExportStats.EXPORT_SPRITES,
                32,
                64,
                onPress,
                msg
        );
        exportButton.setTooltip(Tooltip.create(msg));
        exportButton.setTooltipDelay(500);
        exportButton.active = active;
        return exportButton;
    }
}
