package com.shermansplanet.otherverse.spirits;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

import java.util.HashMap;

public class SpiritAffinityTracker {

    private static HashMap<String, SpiritAffinitySet> playerAffinities = new HashMap<>();

    private static class SpiritAffinitySet {
        private HashMap<SpiritType, Float> baseAffinities = new HashMap<>();
        public SpiritType implementType, familiarType, demesneType;

        public CompoundTag save() {
            var tag = new CompoundTag();
            if (implementType != null) tag.putInt("implementType", implementType.id());
            if (familiarType != null) tag.putInt("familiarType", familiarType.id());
            if (demesneType != null) tag.putInt("demesneType", demesneType.id());
            var baseAffinityTag = new CompoundTag();
            for (var ba : baseAffinities.entrySet()) {
                baseAffinityTag.putFloat(ba.getKey().label(), ba.getValue());
            }
            tag.put("baseAffinities", baseAffinityTag);
            return tag;
        }

        public SpiritAffinitySet(CompoundTag tag) {
            if (tag.contains("implementType")) implementType = Spirits.spiritsById.get(tag.getInt("implementType"));
            if (tag.contains("familiarType")) familiarType = Spirits.spiritsById.get(tag.getInt("familiarType"));
            if (tag.contains("demesneType")) demesneType = Spirits.spiritsById.get(tag.getInt("demesneType"));
            var baseAffinityTag = tag.getCompound("baseAffinities");
            for (var k : baseAffinityTag.getAllKeys()) {
                baseAffinities.put(Spirits.spiritsByLabel.get(k), baseAffinityTag.getFloat(k));
            }
        }

        public SpiritAffinitySet() {
        }

        public void pullAffinityTowards(SpiritType spiritType, float goal) {
            var current = baseAffinities.getOrDefault(spiritType, 0f);
            var lerp = goal > 0 ? 0.1f : 0.2f;
            baseAffinities.put(spiritType, current * (1 - lerp) + goal * lerp);
        }

        public float getTotalFor(SpiritType spiritType) {
            var total = baseAffinities.getOrDefault(spiritType, 0f);
            var opposite = SpiritTransfer.getOppositeSpiritType(spiritType);

            if (implementType == spiritType) total += 0.5f;
            if (familiarType == spiritType) total += 0.5f;
            if (demesneType == spiritType) total += 0.5f;

            if (implementType == opposite) total -= 0.5f;
            if (familiarType == opposite) total -= 0.5f;
            if (demesneType == opposite) total -= 0.5f;

            return total;
        }
    }

    public static void load(CompoundTag tag) {
        for (var k : tag.getAllKeys()) {
            playerAffinities.put(k, new SpiritAffinitySet(tag.getCompound(k)));
        }
    }

    public static CompoundTag save() {
        var tag = new CompoundTag();
        for (var playerName : playerAffinities.keySet()) {
            tag.put(playerName, playerAffinities.get(playerName).save());
        }
        return tag;
    }

    public static void increaseAffinity(SpiritType spiritType, ServerPlayer player) {
        if (spiritType == null) return;
        getAffinityFor(player).pullAffinityTowards(spiritType, 1f);
        sendUpdate(player);
    }

    public static void decreaseAffinity(SpiritType spiritType, ServerPlayer player) {
        if (spiritType == null) return;
        getAffinityFor(player).pullAffinityTowards(spiritType, -1f);
        sendUpdate(player);
    }

    public static void setImplementAffinity(SpiritType spiritType, ServerPlayer player) {
        getAffinityFor(player).implementType = spiritType;
        sendUpdate(player);
    }

    public static void setFamiliarAffinity(SpiritType spiritType, ServerPlayer player) {
        getAffinityFor(player).familiarType = spiritType;
        sendUpdate(player);
    }

    public static void setDemesneAffinity(SpiritType spiritType, ServerPlayer player) {
        getAffinityFor(player).demesneType = spiritType;
        sendUpdate(player);
    }

    private static void sendUpdate(ServerPlayer player) {
    }

    private static SpiritAffinitySet getAffinityFor(ServerPlayer player) {
        return player == null ? null : getAffinityFor(player.getGameProfile().getName());
    }

    private static SpiritAffinitySet getAffinityFor(String playerName) {
        return playerAffinities.computeIfAbsent(playerName, x -> new SpiritAffinitySet());
    }

    public static int getTransferDuration(String playerName, SpiritType spiritType) {
        var base = 100;
        if (playerName == null) return base;
        var affinity = getAffinityFor(playerName).getTotalFor(spiritType);
        return Math.round(base * (float) Math.pow(0.3f, affinity));
    }

    public static float getCoeff(String playerName, SpiritType spiritType) {
        var affinity = getAffinityFor(playerName).getTotalFor(spiritType);
        affinity -= Mth.clamp(affinity, -1f, 1f);
        if (affinity < 0) affinity *= 2;
        return (float) Math.pow(1.32f, affinity);
    }
}
