package com.shermansplanet.otherverse;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.binding.MobTransfusions;
import com.shermansplanet.otherverse.spirits.SpiritTransfusions;
import com.shermansplanet.otherverse.spirits.SpiritType;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.*;

public record PracticeWorldUpdateMessage(
        HashMap<Item, Hashtable<SpiritType, Integer>> spiritMappings,
        HashMap<Item, List<SpiritTransfusions.SpiritTransfusionData>> spiritTransfusions,
        HashMap<ItemOrEntityType, List<MobTransfusions.MobTransfusionData>> mobTransfusions,
        HashMap<EntityType<? extends LivingEntity>, HashMap<ItemOrEntityType, Integer>> bindings,
        HashMap<EntityType<? extends LivingEntity>, SpiritType> mobSpirits,
        HashMap<ResourceLocation, List<SpiritType>> biomeCodes) {

    private static final Logger LOGGER = LogUtils.getLogger();

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeShort(spiritMappings.size());
        for (var mapping : spiritMappings.entrySet()) {
            buffer.writeInt(Item.getId(mapping.getKey()));
            buffer.writeByte(mapping.getValue().size());
            for (var spiritAmounts : mapping.getValue().entrySet()) {
                buffer.writeByte(spiritAmounts.getKey().id());
                buffer.writeShort(spiritAmounts.getValue());
            }
        }
        buffer.writeShort(spiritTransfusions.size());
        for (var transfusions : spiritTransfusions.entrySet()) {
            buffer.writeInt(Item.getId(transfusions.getKey()));
            buffer.writeByte(transfusions.getValue().size());
            for (var transfusion : transfusions.getValue()) {
                buffer.writeItem(transfusion.output());
                buffer.writeShort(transfusion.price());
                buffer.writeByte(transfusion.spiritType().id());
                var hasBlock = transfusion.blockOutput() != null;
                buffer.writeBoolean(hasBlock);
                if (hasBlock) buffer.writeResourceLocation(ForgeRegistries.BLOCKS.getKey(transfusion.blockOutput()));
            }
        }
        buffer.writeShort(mobTransfusions.size());
        for (var transfusions : mobTransfusions.entrySet()) {
            transfusions.getKey().encode(buffer);
            buffer.writeShort(transfusions.getValue().size());
            for (var transfusion : transfusions.getValue()) {
                buffer.writeItem(transfusion.destItem());
                buffer.writeShort(transfusion.price());
                buffer.writeShort(transfusion.entityTypes().size());
                for (var et : transfusion.entityTypes()) {
                    buffer.writeInt(BuiltInRegistries.ENTITY_TYPE.getId(et));
                }
            }
        }
        buffer.writeShort(bindings.size());
        for (var binding : bindings.entrySet()) {
            buffer.writeInt(BuiltInRegistries.ENTITY_TYPE.getId(binding.getKey()));
            buffer.writeShort(binding.getValue().size());
            for (var influence : binding.getValue().entrySet()) {
                influence.getKey().encode(buffer);
                buffer.writeShort(influence.getValue());
            }
        }
        buffer.writeShort(mobSpirits.size());
        for (var mobSpirit : mobSpirits.entrySet()) {
            buffer.writeInt(BuiltInRegistries.ENTITY_TYPE.getId(mobSpirit.getKey()));
            buffer.writeByte(mobSpirit.getValue().id());
        }
        buffer.writeShort(biomeCodes.size());
        for (var biomeCode : biomeCodes.entrySet()) {
            buffer.writeResourceLocation(biomeCode.getKey());
            buffer.writeShort(biomeCode.getValue().size());
            for (var st : biomeCode.getValue()) {
                buffer.writeByte(st.id());
            }
        }
    }

    public static PracticeWorldUpdateMessage decode(FriendlyByteBuf buffer) {
        HashMap<Item, Hashtable<SpiritType, Integer>> spiritMappings = new HashMap<>();
        var mappingsSize = buffer.readShort();
        for (var m = 0; m < mappingsSize; m++) {
            var item = Item.byId(buffer.readInt());
            var table = new Hashtable<SpiritType, Integer>();
            spiritMappings.put(item, table);
            var spiritCount = buffer.readByte();
            for (var s = 0; s < spiritCount; s++) {
                var spiritType = Spirits.spiritsById.get((int) buffer.readByte());
                table.put(spiritType, (int) buffer.readShort());
            }
        }

        HashMap<Item, List<SpiritTransfusions.SpiritTransfusionData>> spiritTransfusions = new HashMap<>();
        var transfusionSize = buffer.readShort();
        for (int i = 0; i < transfusionSize; i++) {
            var item = Item.byId(buffer.readInt());
            var list = new ArrayList<SpiritTransfusions.SpiritTransfusionData>();
            var dataSize = buffer.readByte();
            for (int ii = 0; ii < dataSize; ii++) {
                var output = buffer.readItem();
                var price = buffer.readShort();
                var spiritType = Spirits.spiritsById.get((int) buffer.readByte());
                var hasBlock = buffer.readBoolean();
                if (hasBlock) {
                    Block block = ForgeRegistries.BLOCKS.getValue(buffer.readResourceLocation());
                    list.add(new SpiritTransfusions.SpiritTransfusionData(spiritType, output, price, block));
                } else {
                    list.add(new SpiritTransfusions.SpiritTransfusionData(spiritType, output, price, null));
                }

            }
            spiritTransfusions.put(item, list);
        }

        HashMap<ItemOrEntityType, List<MobTransfusions.MobTransfusionData>> mobTransfusions = new HashMap<>();
        var mobTransfusionSize = buffer.readShort();
        for (var i = 0; i < mobTransfusionSize; i++) {
            var key = ItemOrEntityType.decode(buffer);
            var list = new ArrayList<MobTransfusions.MobTransfusionData>();
            var transfusionCount = buffer.readShort();
            for (var ii = 0; ii < transfusionCount; ii++) {
                var item = buffer.readItem();
                var price = buffer.readShort();
                var etcount = buffer.readShort();
                var etset = new HashSet<EntityType<?>>();
                for (var iii = 0; iii < etcount; iii++) {
                    etset.add(BuiltInRegistries.ENTITY_TYPE.byId(buffer.readInt()));
                }
                list.add(new MobTransfusions.MobTransfusionData(etset, item, price));
            }
            mobTransfusions.put(key, list);
        }

        HashMap<EntityType<? extends LivingEntity>, HashMap<ItemOrEntityType, Integer>> bindings = new HashMap<>();
        var bindingsSize = buffer.readShort();
        for (var i = 0; i < bindingsSize; i++) {
            var et = (EntityType<? extends LivingEntity>) BuiltInRegistries.ENTITY_TYPE.byId(buffer.readInt());
            var influenceSize = buffer.readShort();
            var map = new HashMap<ItemOrEntityType, Integer>();
            for (var ii = 0; ii < influenceSize; ii++) {
                var k = ItemOrEntityType.decode(buffer);
                var v = (Integer) (int) buffer.readShort();
                map.put(k, v);
            }
            bindings.put(et, map);
        }

        HashMap<EntityType<? extends LivingEntity>, SpiritType> mobSpirits = new HashMap<>();
        var mobSpiritsSize = buffer.readShort();
        for (var i = 0; i < mobSpiritsSize; i++) {
            var et = (EntityType<? extends LivingEntity>) BuiltInRegistries.ENTITY_TYPE.byId(buffer.readInt());
            var spiritType = Spirits.spiritsById.get((int) buffer.readByte());
            mobSpirits.put(et, spiritType);
        }

        HashMap<ResourceLocation, List<SpiritType>> biomeCodes = new HashMap<>();
        var biomeCodeSize = buffer.readShort();
        for (var i = 0; i < biomeCodeSize; i++) {
            var biome = buffer.readResourceLocation();
            var spiritTypeSize = buffer.readShort();
            var list = new ArrayList<SpiritType>();
            for (var j = 0; j < spiritTypeSize; j++) {
                list.add(Spirits.spiritsById.get((int) buffer.readByte()));
            }
            biomeCodes.put(biome, list);
        }

        return new PracticeWorldUpdateMessage(spiritMappings, spiritTransfusions, mobTransfusions, bindings, mobSpirits, biomeCodes);
    }
}
