package org.moshang.tempusetchaos.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.data.EntropyWorldData;

@EventBusSubscriber(modid = TempusEtChaos.MODID)
public class TestCommand {
    @SubscribeEvent
    public static void onServerStarting(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("entropy")
                        .then(Commands.literal("set")
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .then(Commands.argument("value", FloatArgumentType.floatArg(0, 100))
                                                        .executes(context -> {
                                                            ServerLevel level = context.getSource().getLevel();
                                                            int x = IntegerArgumentType.getInteger(context, "x");
                                                            int z = IntegerArgumentType.getInteger(context, "z");
                                                            float value = FloatArgumentType.getFloat(context, "value");
                                                            EntropyWorldData.get(level).setConcentration(new ChunkPos(x, z), value);
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("debug")
                                .executes(context -> {
                                    ServerLevel level = context.getSource().getLevel();
                                    EntropyWorldData data = EntropyWorldData.get(level);
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("Concentrations: " + data.getConcentrations()), false);
                                    return 1;
                                })
                        )
        );
    }
}
