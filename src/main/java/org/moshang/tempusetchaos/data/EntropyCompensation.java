package org.moshang.tempusetchaos.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The concentration tiers of the entropy reactor and what each one pays. Tiers are data: every file under
 * {@code data/<namespace>/entropy_reactor_tiers/} is one tier, and the highest {@code min_concentration} that the chunk
 * reaches wins. The reactor's base rates stay hardcoded; only the rewards are open to packs.
 */
@Slf4j
@ParametersAreNonnullByDefault
public final class EntropyCompensation extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "entropy_reactor_tiers";
    public static final EntropyCompensation INSTANCE = new EntropyCompensation();

    private static volatile List<Tier> tiers = List.of();

    private EntropyCompensation() {
        super(new Gson(), DIRECTORY);
    }

    /** The tier a chunk's concentration falls in, or null below the lowest tier. */
    @Nullable
    public static Tier tierFor(float concentration) {
        Tier found = null;
        for (Tier tier : tiers) {
            if (tier.minConcentration() > concentration) break;
            found = tier;
        }
        return found;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Float, Tier> byMin = new TreeMap<>();
        files.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> Tier.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                        .resultOrPartial(error -> log.warn("Skipping entropy reactor tier {}: {}", entry.getKey(), error))
                        .ifPresent(tier -> {
                            if (byMin.put(tier.minConcentration(), tier) != null) {
                                log.warn("Duplicate entropy reactor tier at concentration {}; {} wins",
                                        tier.minConcentration(), entry.getKey());
                            }
                        }));
        tiers = List.copyOf(byMin.values());
    }

    /** One concentration threshold and its rewards. */
    public record Tier(float minConcentration, List<Output> outputs) {
        public static final Codec<Tier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("min_concentration").forGetter(Tier::minConcentration),
                Output.CODEC.listOf().optionalFieldOf("outputs", List.of()).forGetter(Tier::outputs)
        ).apply(instance, Tier::new));

        public Tier {
            minConcentration = Mth.clamp(minConcentration, 0f, 100f);
            outputs = List.copyOf(outputs);
        }
    }

    /** One possible result of a tier; chance 1 always pays. */
    public record Output(ItemStack stack, float chance) {
        public static final Codec<Output> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(output -> output.stack().getItem()),
                Codec.INT.optionalFieldOf("count", 1).forGetter(output -> output.stack().getCount()),
                Codec.FLOAT.optionalFieldOf("chance", 1f).forGetter(Output::chance)
        ).apply(instance, (item, count, chance) -> new Output(new ItemStack(item, count), chance)));

        public Output {
            chance = Mth.clamp(chance, 0f, 1f);
            stack = new ItemStack(stack.getItem(), Mth.clamp(stack.getCount(), 1, 64));
        }
    }
}
