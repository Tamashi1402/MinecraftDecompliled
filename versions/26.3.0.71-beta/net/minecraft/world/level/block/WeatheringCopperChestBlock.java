package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public class WeatheringCopperChestBlock extends CopperChestBlock implements WeatheringCopper {
    public WeatheringCopperChestBlock(
        WeatheringCopper.WeatherState weatherState, SoundEvent openSound, SoundEvent closeSound, BlockBehaviour.Properties properties
    ) {
        super(weatherState, openSound, closeSound, properties);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return WeatheringCopper.getNext(state.getBlock()).isPresent();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ChestBlock.TYPE).equals(ChestType.RIGHT)
            && level.getBlockEntity(pos) instanceof ChestBlockEntity chestBlockEntity
            && chestBlockEntity.getEntitiesWithContainerOpen().isEmpty()) {
            this.changeOverTime(state, level, pos, random);
        }
    }

    public WeatheringCopper.WeatherState getAge() {
        return this.getState();
    }

    @Override
    public boolean isWaxed() {
        return false;
    }
}
