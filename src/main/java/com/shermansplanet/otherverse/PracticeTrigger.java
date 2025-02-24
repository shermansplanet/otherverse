package com.shermansplanet.otherverse;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class PracticeTrigger extends SimpleCriterionTrigger<PracticeTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation(Otherverse.MODID, "perform_practice");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, EntityPredicate.Composite predicate, DeserializationContext ctx) {
        var practice = json.get("practice").getAsString();
        return new TriggerInstance(practice, predicate);
    }

    public void trigger(ServerPlayer player, String practiceName) {
        if(player == null) return;
        this.trigger(player, (p_59481_) -> p_59481_.matches(practiceName));
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {

        private String practiceName;

        public TriggerInstance(String practice, EntityPredicate.Composite p_16976_) {
            super(PracticeTrigger.ID, p_16976_);
            this.practiceName = practice;
        }

        public boolean matches(String practiceName) {
            return this.practiceName.equals(practiceName);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext p_59513_) {
            JsonObject jsonobject = super.serializeToJson(p_59513_);
            jsonobject.addProperty("practice", practiceName);
            return jsonobject;
        }
    }
}
