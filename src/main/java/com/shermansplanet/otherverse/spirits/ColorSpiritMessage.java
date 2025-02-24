package com.shermansplanet.otherverse.spirits;

import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;

import java.util.HashMap;

public class ColorSpiritMessage {

    public final HashMap<Item, SpiritLabeler.SpiritAmount[]> colorSpiritMappings;

    public ColorSpiritMessage(HashMap<Item, SpiritLabeler.SpiritAmount[]> sto) {
        colorSpiritMappings = sto;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(colorSpiritMappings.size());
        for (var entry : colorSpiritMappings.entrySet()) {
            buffer.writeId(Registry.ITEM, entry.getKey());
            buffer.writeByte(entry.getValue().length);
            for (var i = 0; i < entry.getValue().length; i++) {
                var spirit = entry.getValue()[i];
                buffer.writeByte(spirit.type().id());
                buffer.writeInt(spirit.amount());
            }
        }
    }

    public static ColorSpiritMessage decode(FriendlyByteBuf buffer) {
        var sto = new HashMap<Item, SpiritLabeler.SpiritAmount[]>();
        var itemCount = buffer.readInt();
        for (var i = 0; i < itemCount; i++) {
            var item = buffer.readById(Registry.ITEM);
            var spiritCount = buffer.readByte();
            var spirits = new SpiritLabeler.SpiritAmount[spiritCount];
            for (var ii = 0; ii < spiritCount; ii++) {
                var spiritType = Spirits.spiritsById.get((int) buffer.readByte());
                int amount = buffer.readInt();
                spirits[ii] = new SpiritLabeler.SpiritAmount(spiritType, amount);
            }
            sto.put(item, spirits);
        }
        return new ColorSpiritMessage(sto);
    }
}
