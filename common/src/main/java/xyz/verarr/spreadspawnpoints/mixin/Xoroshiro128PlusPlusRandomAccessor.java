package xyz.verarr.spreadspawnpoints.mixin;

import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandomImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Xoroshiro128PlusPlusRandom.class)
public interface Xoroshiro128PlusPlusRandomAccessor {
    @Accessor("implementation")
    Xoroshiro128PlusPlusRandomImpl getImplementation();
}
