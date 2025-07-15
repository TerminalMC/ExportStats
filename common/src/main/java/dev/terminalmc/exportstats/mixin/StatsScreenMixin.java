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
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
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

import java.time.Duration;

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

    @Shadow
    @Final
    private static Component GENERAL_BUTTON;

    @Shadow
    @Final
    private static Component ITEMS_BUTTON;

    @Shadow
    @Final
    private static Component MOBS_BUTTON;

    @Shadow
    @Final
    private static int PADDING;

    public StatsScreenMixin(Component text) {
        super(text);
    }

    /**
     * Adds the export buttons in their respective positions.
     */
    @WrapOperation(
            method = "initButtons",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"
            )
    )
    public LayoutElement wrapAddChild(
            LinearLayout instance,
            LayoutElement child,
            Operation<LayoutElement> original
    ) {
        int iconButtonSize = 20;
        int exportAllButtonWidth = 60;

        LayoutElement retVal;
        if (child instanceof Button button) {
            if (button.getMessage().equals(GENERAL_BUTTON)) {
                button.setWidth(button.getWidth() - (iconButtonSize + PADDING));
                retVal = original.call(instance, button);
                Button exportButton = exportstats$createIconButton(
                        localized("button", "export.general.tooltip"),
                        (b) -> {
                            if (statsList != null && !statsList.children().isEmpty())
                                ExportStats.saveStats(stats, statsList, true);
                        },
                        statsList != null && !statsList.children().isEmpty()
                );
                instance.addChild(exportButton);

            } else if (button.getMessage().equals(ITEMS_BUTTON)) {
                button.setWidth(button.getWidth() - (iconButtonSize + PADDING));
                retVal = original.call(instance, button);
                Button exportButton = exportstats$createIconButton(
                        localized("button", "export.items.tooltip"),
                        (b) -> {
                            if (itemStatsList != null && !itemStatsList.children().isEmpty())
                                ExportStats.saveStats(stats, itemStatsList, true);
                        },
                        itemStatsList != null && !itemStatsList.children().isEmpty()
                );
                instance.addChild(exportButton);

            } else if (button.getMessage().equals(MOBS_BUTTON)) {
                button.setWidth(button.getWidth() - (iconButtonSize + PADDING));
                retVal = original.call(instance, button);
                Button exportButton = exportstats$createIconButton(
                        localized("button", "export.mobs.tooltip"),
                        (b) -> {
                            if (mobsStatsList != null && !mobsStatsList.children().isEmpty())
                                ExportStats.saveStats(stats, mobsStatsList, true);
                        },
                        mobsStatsList != null && !mobsStatsList.children().isEmpty()
                );
                instance.addChild(exportButton);

            } else if (button.getMessage().equals(CommonComponents.GUI_DONE)) {
                button.setWidth(button.getWidth() - exportAllButtonWidth);
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
                LinearLayout layout = LinearLayout.horizontal().spacing(PADDING);
                layout.addChild(exportAllButton);
                layout.addChild(button);
                original.call(instance, layout);
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
    private @NotNull Button exportstats$createIconButton(
            Component msg,
            OnPress onPress,
            boolean active
    ) {
        Button exportButton = new ImageButton(20, 20, ExportStats.EXPORT_SPRITES, onPress, msg);
        exportButton.setTooltip(Tooltip.create(msg));
        exportButton.setTooltipDelay(Duration.ofMillis(500));
        exportButton.active = active;
        return exportButton;
    }
}
