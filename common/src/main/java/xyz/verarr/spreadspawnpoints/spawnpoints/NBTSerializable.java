package xyz.verarr.spreadspawnpoints.spawnpoints;

import net.minecraft.nbt.NbtCompound;

public interface NBTSerializable {
    NbtCompound writeNbt();

    void modifyFromNbt(NbtCompound tag);

    void modifyFromNbtPartial(NbtCompound tag) throws IllegalArgumentException;
}
