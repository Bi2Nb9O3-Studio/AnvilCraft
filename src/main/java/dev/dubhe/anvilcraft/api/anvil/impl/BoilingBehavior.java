package dev.dubhe.anvilcraft.api.anvil.impl;

import dev.dubhe.anvilcraft.api.anvil.AnvilBehavior;
import dev.dubhe.anvilcraft.api.event.entity.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.util.AnvilUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BoilingBehavior implements AnvilBehavior {
    @Override
    public void handle(
            Level level,
            BlockPos hitBlockPos,
            BlockState hitBlockState,
            float fallDistance,
            AnvilFallOnLandEvent event) {
        BlockState belowState = level.getBlockState(hitBlockPos.below());
        if (belowState.is(Blocks.CAMPFIRE) && belowState.getValue(CampfireBlock.LIT)) {
            AnvilUtil.itemProcess(ModRecipeTypes.BOILING_TYPE.get(), level, hitBlockPos, hitBlockPos.getCenter());
        }
    }
}
