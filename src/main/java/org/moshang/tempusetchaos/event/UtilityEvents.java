package org.moshang.tempusetchaos.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.moshang.tempusetchaos.TempusEtChaos;

@EventBusSubscriber
public class UtilityEvents {
    private static final ResourceLocation SLOWDOWN = ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "chronon_slowdown");

    @SubscribeEvent
    public static void LivingSlowdown(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) return;

        if (entity instanceof LivingEntity living) {
            CompoundTag data = entity.getPersistentData();
            if (data.contains("chronon_slowdown")) {
                int factor = data.getInt("chronon_slowdown");
                AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null && !speed.hasModifier(SLOWDOWN)) {
                    speed.addTransientModifier(
                            new AttributeModifier(SLOWDOWN, -(1 - 1.0 / factor), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    );
                }
            } else {
                AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null && speed.hasModifier(SLOWDOWN))
                    speed.removeModifier(SLOWDOWN);
            }
        }
    }
}
