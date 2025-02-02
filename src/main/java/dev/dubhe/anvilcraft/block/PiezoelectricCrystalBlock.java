package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager.Entry;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PiezoelectricCrystalBlock extends Block implements IHammerRemovable {
    public static VoxelShape SHAPE =
        Shapes.or(Block.box(0, 14, 0, 16, 16, 16), Block.box(2, 2, 2, 14, 14, 14), Block.box(0, 0, 0, 16, 2, 16));

    public PiezoelectricCrystalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context) {
        return SHAPE;
    }

    /**
     * 被铁砧砸事件
     */
    public void onHitByAnvil(float fallDistance, Level level, BlockPos blockPos) {
        int distance = (int) Math.min(3, fallDistance);
        int chargeNum = (int) Math.pow(2, distance);
        this.charge(chargeNum, level, blockPos);
        pressureConduction(level, blockPos, chargeNum / 2, 4);
    }

    private void charge(int chargeNum, Level level, BlockPos blockPos) {
        Collection<Entry> chargeCollectorCollection =
            ChargeCollectorManager.getInstance(level).getNearestChargeCollect(blockPos);
        double surplus = chargeNum;
        for (Entry entry : chargeCollectorCollection) {
            ChargeCollectorBlockEntity chargeCollectorBlockEntity = entry.getBlockEntity();
            if (!ChargeCollectorManager.getInstance(level).canCollect(chargeCollectorBlockEntity, blockPos)) return;
            surplus = chargeCollectorBlockEntity.incomingCharge(surplus, blockPos);
            if (surplus == 0) return;
        }
    }

    private void pressureConduction(Level level, BlockPos blockPos, int chargeNum, int count) {
        int c = count - 1;
        if (c <= 0) return;
        BlockPos pos = blockPos.below();
        if (level.getBlockState(pos).getBlock() instanceof PiezoelectricCrystalBlock block) {
            if (chargeNum == 0) return;
            this.charge(chargeNum, level, blockPos);
            block.pressureConduction(level, pos, chargeNum / 2, c);
        }
    }
}
