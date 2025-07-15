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

package dev.terminalmc.exportstats.mixin.world;

import dev.terminalmc.exportstats.ExportStats;
import net.minecraft.client.Minecraft;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    /**
     * Retrieves a singleplayer world name.
     */
    @Inject(
            method = "doWorldLoad",
            at = @At("HEAD")
    )
    private void onWorldLoad(
            String levelName,
            LevelStorageSource.LevelStorageAccess levelStorage,
            PackRepository packRepo,
            WorldStem worldStem,
            boolean newWorld,
            CallbackInfo ci
    ) {
        ExportStats.lastWorld = worldStem.worldData().getLevelName();
        ExportStats.lastWorldIsServer = false;
    }
}
