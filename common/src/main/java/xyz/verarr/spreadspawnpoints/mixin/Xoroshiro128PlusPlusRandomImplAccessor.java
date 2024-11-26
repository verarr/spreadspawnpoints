package xyz.verarr.spreadspawnpoints.mixin;

import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandomImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Xoroshiro128PlusPlusRandomImpl.class)
public interface Xoroshiro128PlusPlusRandomImplAccessor {
    @Accessor("seedHi")
    long getSeedHi();
    @Accessor("seedLo")
    long getSeedLo();
}
