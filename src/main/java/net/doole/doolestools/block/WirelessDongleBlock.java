package net.doole.doolestools.block;

import com.mojang.serialization.MapCodec;
import net.doole.doolestools.blockentity.WirelessDongleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class WirelessDongleBlock extends NetworkEndpointBlock {
    public static final MapCodec<WirelessDongleBlock> CODEC = simpleCodec(WirelessDongleBlock::new);

    public WirelessDongleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends NetworkEndpointBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WirelessDongleBlockEntity(pos, state);
    }
}
