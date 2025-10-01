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
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.LinearLayout.Orientation;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
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
    @Final
    StatsCounter stats;

    @Shadow
    @Nullable
    private TabNavigationBar tabNavigationBar;

    public StatsScreenMixin(Component text) {
        super(text);
    }

    @WrapOperation(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToFooter(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"
            )
    )
    private <T extends LayoutElement> T wrapAddDoneButton(
            HeaderAndFooterLayout instance,
            T child,
            Operation<T> original
    ) {
        LinearLayout layout = new LinearLayout(width, child.getHeight(), Orientation.HORIZONTAL);
        layout.addChild(child);
        Button exportButton = exportstats$createIconButton(
                localized("button", "export.all"),
                (b) -> exportstats$saveStats()
        );
        layout.addChild(exportButton);
        original.call(instance, layout);
        return null;
    }

    @Unique
    private void exportstats$saveStats() {
        if (tabNavigationBar != null) {
            ExportStats.saveStats(
                    stats,
                    tabNavigationBar.getTabs(),
                    true
            );
        }
    }

    @Unique
    private @NotNull Button exportstats$createIconButton(
            Component msg,
            OnPress onPress
    ) {
        Button exportButton = new ImageButton(20, 20, ExportStats.EXPORT_SPRITES, onPress, msg);
        exportButton.setTooltip(Tooltip.create(msg));
        exportButton.setTooltipDelay(Duration.ofMillis(500));
        return exportButton;
    }
}
