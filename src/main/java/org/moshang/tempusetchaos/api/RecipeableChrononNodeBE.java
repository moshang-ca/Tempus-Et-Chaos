package org.moshang.tempusetchaos.api;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.api.storage.NotifiedItemStackHandler;
import org.moshang.tempusetchaos.recipe.IProcessingRecipe;
import org.moshang.tempusetchaos.recipe.IProcessingRecipe.Results;
import org.moshang.tempusetchaos.recipe.IProcessingRecipe.Values;
import org.moshang.tempusetchaos.recipe.MaterialLedger;
import org.moshang.tempusetchaos.recipe.ProcessingRecipe;
import org.moshang.tempusetchaos.recipe.RecipeMaterials;
import org.moshang.tempusetchaos.recipe.RecipeMaterials.FluidAmount;
import org.moshang.tempusetchaos.recipe.RecipeMaterials.ItemRequirement;
import org.moshang.tempusetchaos.recipe.input.MaterialsInput;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

/**
 * A node that runs a processing recipe. A machine declares where its materials and values live, and where its results
 * go; this class does the matching, the per-tick accounting and the storage handling itself.
 * <p>
 * The promise of a tick never changes: it either moves everything it should or it leaves no trace at all. Costs are
 * therefore checked before anything is taken, and results are fitted into a copy of the output storage before any of
 * them is handed out.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class RecipeableChrononNodeBE<R extends ProcessingRecipe> extends BaseChrononNodeBlockEntity {
    protected final RecipeType<R> recipeType;
    @Getter
    protected int progress = 0;
    private int processingTime = 0;
    private int chrononBalance = 0;
    private int runningParallel = 1;
    protected int retryCooldown = 0;
    protected int saveCooldown = 0;
    protected boolean recipeDirty = true;
    @Getter
    protected boolean recipeRunning = false;
    @Getter
    protected R recipe = null;
    @Setter
    protected RecipeModifiers modifiers = RecipeModifiers.NONE;
    @Nullable
    private ResourceLocation recipeId = null;
    /** The recipe that ran last, kept for the cheap match after a run instead of sweeping the recipe manager. */
    @Nullable
    private RecipeHolder<R> lastRecipe;
    @Nullable
    private IValueChannel chrononChannel;

    public RecipeableChrononNodeBE(BlockEntityType<?> type, BlockPos pos, BlockState blockState,
                                   int chrononCapacity, RecipeType<R> recipeType) {
        super(type, pos, blockState, chrononCapacity);
        this.recipeType = recipeType;
    }

    /** Item slots the machine takes its materials from. */
    protected List<NotifiedItemStackHandler> itemInputs() {
        return List.of();
    }

    /** Item slots the machine puts its results into. */
    protected List<NotifiedItemStackHandler> itemOutputs() {
        return List.of();
    }

    /** Tanks the machine takes its fluids from. */
    protected List<IFluidHandler> fluidInputs() {
        return List.of();
    }

    /** Tanks the machine puts its fluid results into. */
    protected List<IFluidHandler> fluidOutputs() {
        return List.of();
    }

    /** The energy of this machine, null when it has none. */
    @Nullable
    protected IValueChannel energyChannel() {
        return null;
    }

    protected IValueChannel chrononChannel() {
        if (chrononChannel == null) chrononChannel = new ChrononChannel();
        return chrononChannel;
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (recipe == null && !startRecipe()) {
            recipeRunning = false;
            return;
        }
        recipeRunning = canRun(recipe) && advance(recipe);
        if (++saveCooldown >= 20) {
            saveCooldown = 0;
            setChanged();
        }
    }

    /** Ticks the running recipe needs as the machine stands now; on the client this is what the menu mirrored. */
    public int getProcessingTime() {
        return recipe != null ? effectiveTime(recipe) : processingTime;
    }

    /** Fraction of the running recipe already done, 0 while idle. */
    public float getProgressRatio() {
        int time = getProcessingTime();
        return time <= 0 ? 0f : Mth.clamp(progress / (float) time, 0f, 1f);
    }

    /** Chronon the running recipe moves per tick on average: positive pays out, negative takes in, 0 while idle. */
    public float getChrononRate() {
        int time = getProcessingTime();
        if (time <= 0) return 0f;
        return (recipe != null ? chrononTotal(recipe, time) : chrononBalance) / (float) time;
    }

    /** The menu's view of the recipe: the progress, the ticks it will take and its chronon balance. */
    public ContainerData recipeData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> progress;
                    case 1 -> getProcessingTime();
                    default -> recipe != null ? (int) chrononTotal(recipe, getProcessingTime()) : chrononBalance;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> progress = value;
                    case 1 -> processingTime = value;
                    default -> chrononBalance = value;
                }
            }

            @Override
            public int getCount() {
                return 3;
            }
        };
    }

    private int effectiveTime(R recipe) {
        return Math.max(1, Math.round(recipe.processingTime() * modifiers.timeScale()));
    }

    private long chrononTotal(R recipe, int time) {
        float scale = modifiers.valueScale();
        return recipe.valuesOut().chronon().total(time, runningParallel, scale)
                - recipe.valuesIn().chronon().total(time, runningParallel, scale);
    }

    private boolean advance(R recipe) {
        boolean finishing = progress + 1 >= effectiveTime(recipe);
        if (!apply(tickPlan(recipe, finishing, runningParallel))) return false;
        progress++;
        if (finishing) endRecipe();
        return true;
    }

    private boolean startRecipe() {
        if (recipeId != null) {
            RecipeHolder<R> resumed = findById(recipeId);
            if (resumed != null) {
                recipe = resumed.value();
                lastRecipe = resumed;
                return true;
            }
            recipeId = null;
        }
        // The recipe that just ran is the cheapest candidate: matching it needs no recipe-manager sweep.
        if (lastRecipe != null && tryStart(lastRecipe)) return true;
        if (recipeDirty) {
            recipeDirty = false;
        } else if (--retryCooldown > 0) {
            return false;
        }
        assert level != null;
        RecipeHolder<R> found = level.getRecipeManager().getRecipeFor(recipeType, materialsInput(), level).orElse(null);
        retryCooldown = 20;
        return found != null && tryStart(found);
    }

    /** Starts the holder when its materials still match and its start costs fit, and keeps it as the cached recipe. */
    private boolean tryStart(RecipeHolder<R> holder) {
        assert level != null;
        R value = holder.value();
        if (!canRun(value) || !value.matches(materialsInput(), level)) return false;
        // as many batches as the machine can really afford, up to what its upgrades allow
        for (int parallel = modifiers.maxParallel(); parallel > 0; parallel--) {
            if (!apply(startPlan(value, parallel))) continue;
            recipe = value;
            recipeId = holder.id();
            lastRecipe = holder;
            runningParallel = parallel;
            progress = 0;
            setChanged();
            return true;
        }
        return false;
    }

    private void endRecipe() {
        recipe = null;
        recipeId = null;
        progress = 0;
        runningParallel = 1;
        setChanged();
        startRecipe();
    }

    @Nullable
    private RecipeHolder<R> findById(ResourceLocation id) {
        assert level != null;
        for (RecipeHolder<R> holder : level.getRecipeManager().getAllRecipesFor(recipeType)) {
            if (holder.id().equals(id)) return holder;
        }
        return null;
    }

    /** Extra condition for processing, checked every tick. */
    protected boolean canRun(R recipe) {
        return true;
    }

    /** What starting the recipe costs: every one-shot cost of both layers. */
    private TickPlan startPlan(R recipe, int parallel) {
        Values values = recipe.valuesIn();
        return new TickPlan(rollInputs(recipe.inputs(), false, parallel), List.of(), List.of(),
                oneShot(values.energy(), parallel), oneShot(values.chronon(), parallel), 0, 0);
    }

    /** What one tick moves: its per-tick work, plus the one-shot results on the finishing tick. */
    private TickPlan tickPlan(R recipe, boolean finishing, int parallel) {
        Values in = recipe.valuesIn();
        Values out = recipe.valuesOut();
        List<ItemStack> itemResults = new ArrayList<>(rollItemResults(recipe.outputs(), true, parallel));
        List<FluidStack> fluidResults = new ArrayList<>(rollFluidResults(recipe.outputs(), true, parallel));
        if (finishing) {
            itemResults.addAll(rollItemResults(recipe.outputs(), false, parallel));
            fluidResults.addAll(rollFluidResults(recipe.outputs(), false, parallel));
        }
        return new TickPlan(rollInputs(recipe.inputs(), true, parallel), itemResults, fluidResults,
                perTick(in.energy(), parallel), perTick(in.chronon(), parallel),
                payout(out.energy(), parallel, finishing), payout(out.chronon(), parallel, finishing));
    }

    private long perTick(IProcessingRecipe.ValueQuantity value, int parallel) {
        return value.perTick() ? value.amount(parallel, modifiers.valueScale()) : 0;
    }

    private long payout(IProcessingRecipe.ValueQuantity value, int parallel, boolean finishing) {
        return value.perTick() || finishing ? value.amount(parallel, modifiers.valueScale()) : 0;
    }

    private long oneShot(IProcessingRecipe.ValueQuantity value, int parallel) {
        return value.perTick() ? 0 : value.amount(parallel, modifiers.valueScale());
    }

    private RecipeMaterials rollInputs(RecipeMaterials source, boolean perTick, int parallel) {
        List<ItemRequirement> items = new ArrayList<>();
        for (ItemRequirement requirement : source.items()) {
            if (requirement.perTick() == perTick && roll(requirement.chance())) items.add(requirement.scaled(parallel));
        }
        List<FluidAmount> fluids = new ArrayList<>();
        for (FluidAmount amount : source.fluids()) {
            if (amount.perTick() == perTick && roll(amount.chance())) fluids.add(amount.scaled(parallel));
        }
        return new RecipeMaterials(items, fluids);
    }

    private List<ItemStack> rollItemResults(Results source, boolean perTick, int parallel) {
        List<ItemStack> results = new ArrayList<>();
        for (IProcessingRecipe.ItemResult result : source.items()) {
            if (result.perTick() == perTick && roll(result.chance())) {
                results.add(result.stack().copyWithCount(result.stack().getCount() * parallel));
            }
        }
        return results;
    }

    private List<FluidStack> rollFluidResults(Results source, boolean perTick, int parallel) {
        List<FluidStack> results = new ArrayList<>();
        for (FluidAmount amount : source.fluids()) {
            if (amount.perTick() == perTick && roll(amount.chance())) {
                results.add(amount.stack().copyWithAmount(amount.stack().getAmount() * parallel));
            }
        }
        return results;
    }

    private static boolean roll(float chance) {
        if (chance >= 1f) return true;
        if (chance <= 0f) return false;
        return TempusEtChaos.RANDOM.nextFloat() < chance;
    }

    /** Moves one plan: every cost and every result is checked before the first of them is committed. */
    private boolean apply(TickPlan plan) {
        MaterialLedger ledger = ledger();
        IValueChannel energy = energyChannel();
        IValueChannel chronon = chrononChannel();
        if (!ledger.canTake(plan.inputs(), 1)) return false;
        if (!canPay(energy, plan.energyIn()) || !canPay(chronon, plan.chrononIn())) return false;
        if (!canReceive(energy, plan.energyOut()) || !canReceive(chronon, plan.chrononOut())) return false;

        ResultBuffer buffer = new ResultBuffer();
        if (!buffer.place(plan.itemResults(), plan.fluidResults())) return false;

        ledger.take(plan.inputs(), 1);
        pay(energy, plan.energyIn());
        pay(chronon, plan.chrononIn());
        buffer.commit();
        receive(energy, plan.energyOut());
        receive(chronon, plan.chrononOut());
        return true;
    }

    private static boolean canPay(@Nullable IValueChannel channel, long amount) {
        return amount <= 0 || (channel != null && channel.extract(amount, true) >= amount);
    }

    private static boolean canReceive(@Nullable IValueChannel channel, long amount) {
        return amount <= 0 || (channel != null && channel.insert(amount, true) >= amount);
    }

    private static void pay(@Nullable IValueChannel channel, long amount) {
        if (channel != null && amount > 0) channel.extract(amount, false);
    }

    private static void receive(@Nullable IValueChannel channel, long amount) {
        if (channel != null && amount > 0) channel.insert(amount, false);
    }

    /** The material layer this machine offers for matching, built from the storage it declares. */
    private MaterialsInput materialsInput() {
        List<ItemStack> items = new ArrayList<>();
        for (NotifiedItemStackHandler handler : itemInputs()) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                items.add(handler.getStackInSlot(slot));
            }
        }
        List<FluidStack> fluids = new ArrayList<>();
        for (IFluidHandler handler : fluidInputs()) {
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                fluids.add(handler.getFluidInTank(tank));
            }
        }
        return new MaterialsInput(items, fluids);
    }

    /** The live storage, so that what matching called available can really be taken. */
    private MaterialLedger ledger() {
        List<MaterialLedger.ItemSource> items = new ArrayList<>();
        for (NotifiedItemStackHandler handler : itemInputs()) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                int index = slot;
                items.add(new MaterialLedger.ItemSource() {
                    @Override
                    public ItemStack peek() {
                        return handler.getStackInSlot(index);
                    }

                    @Override
                    public ItemStack extract(int amount, boolean simulate) {
                        return handler.forceExtract(index, amount, simulate);
                    }
                });
            }
        }
        List<MaterialLedger.FluidSource> fluids = new ArrayList<>();
        for (IFluidHandler handler : fluidInputs()) {
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                int index = tank;
                fluids.add(new MaterialLedger.FluidSource() {
                    @Override
                    public FluidStack peek() {
                        return handler.getFluidInTank(index);
                    }

                    @Override
                    public FluidStack drain(int amount, boolean simulate) {
                        return handler.drain(amount, simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
                    }
                });
            }
        }
        return MaterialLedger.of(items, fluids);
    }

    /** A copy of the result storage: a tick's results are fitted here first and only written back when all fit. */
    private final class ResultBuffer {
        private final List<NotifiedItemStackHandler> itemHandlers = itemOutputs();
        private final List<IFluidHandler> fluidHandlers = fluidOutputs();
        private final List<List<ItemStack>> items = new ArrayList<>();
        private final List<List<FluidStack>> fluids = new ArrayList<>();

        private ResultBuffer() {
            for (NotifiedItemStackHandler handler : itemHandlers) {
                List<ItemStack> slots = new ArrayList<>(handler.getSlots());
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    slots.add(handler.getStackInSlot(slot).copy());
                }
                items.add(slots);
            }
            for (IFluidHandler handler : fluidHandlers) {
                List<FluidStack> tanks = new ArrayList<>(handler.getTanks());
                for (int tank = 0; tank < handler.getTanks(); tank++) {
                    tanks.add(FluidStack.EMPTY);
                }
                fluids.add(tanks);
            }
        }

        private boolean place(List<ItemStack> itemResults, List<FluidStack> fluidResults) {
            for (ItemStack stack : itemResults) {
                if (!placeItem(stack)) return false;
            }
            for (FluidStack stack : fluidResults) {
                if (!placeFluid(stack)) return false;
            }
            return true;
        }

        /** Merges into matching stacks first, then fills empty slots, the way a handler would. */
        private boolean placeItem(ItemStack stack) {
            int remaining = stack.getCount();
            for (int pass = 0; pass < 2 && remaining > 0; pass++) {
                for (int handler = 0; handler < items.size() && remaining > 0; handler++) {
                    List<ItemStack> slots = items.get(handler);
                    for (int slot = 0; slot < slots.size() && remaining > 0; slot++) {
                        ItemStack current = slots.get(slot);
                        if (current.isEmpty() != (pass == 1)) continue;
                        if (pass == 0 && !ItemStack.isSameItemSameComponents(current, stack)) continue;
                        int free = Math.min(itemHandlers.get(handler).getSlotLimit(slot), stack.getMaxStackSize())
                                - current.getCount();
                        if (free <= 0) continue;
                        int moved = Math.min(free, remaining);
                        slots.set(slot, current.isEmpty() ? stack.copyWithCount(moved) : current.copyWithCount(current.getCount() + moved));
                        remaining -= moved;
                    }
                }
            }
            return remaining <= 0;
        }

        private boolean placeFluid(FluidStack stack) {
            int remaining = stack.getAmount();
            for (int handler = 0; handler < fluids.size() && remaining > 0; handler++) {
                IFluidHandler tanks = fluidHandlers.get(handler);
                for (int tank = 0; tank < tanks.getTanks() && remaining > 0; tank++) {
                    FluidStack current = tanks.getFluidInTank(tank);
                    if (!current.isEmpty() && !FluidStack.isSameFluidSameComponents(current, stack)) continue;
                    if (!tanks.isFluidValid(tank, stack)) continue;
                    int free = tanks.getTankCapacity(tank) - current.getAmount() - fluids.get(handler).get(tank).getAmount();
                    if (free <= 0) continue;
                    int moved = Math.min(free, remaining);
                    FluidStack planned = fluids.get(handler).get(tank);
                    FluidStack updated = planned.isEmpty()
                            ? stack.copyWithAmount(moved)
                            : planned.copyWithAmount(planned.getAmount() + moved);
                    fluids.get(handler).set(tank, updated);
                    remaining -= moved;
                }
            }
            return remaining <= 0;
        }

        private void commit() {
            for (int handler = 0; handler < itemHandlers.size(); handler++) {
                NotifiedItemStackHandler slots = itemHandlers.get(handler);
                List<ItemStack> planned = items.get(handler);
                for (int slot = 0; slot < planned.size(); slot++) {
                    if (ItemStack.matches(planned.get(slot), slots.getStackInSlot(slot))) continue;
                    slots.setStackInSlot(slot, planned.get(slot));
                }
            }
            for (int handler = 0; handler < fluidHandlers.size(); handler++) {
                IFluidHandler tanks = fluidHandlers.get(handler);
                for (int tank = 0; tank < fluids.get(handler).size(); tank++) {
                    FluidStack planned = fluids.get(handler).get(tank);
                    if (!planned.isEmpty()) tanks.fill(planned, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getInt("progress");
        runningParallel = Math.max(1, tag.getInt("parallel"));
        recipeDirty = true;
        if (tag.contains("recipe")) recipeId = ResourceLocation.parse(tag.getString("recipe"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("progress", progress);
        tag.putInt("parallel", runningParallel);
        if (recipeId != null) tag.putString("recipe", recipeId.toString());
    }

    /** The chronon of this node's network. */
    public final class ChrononChannel implements IValueChannel {
        @Override
        public long stored() {
            return innerNetwork == null ? 0 : innerNetwork.getChrononStored();
        }

        @Override
        public long extract(long amount, boolean simulate) {
            return innerNetwork == null ? 0 : innerNetwork.extractChronon(amount, simulate);
        }

        @Override
        public long insert(long amount, boolean simulate) {
            return innerNetwork == null ? 0 : innerNetwork.receiveChronon(amount, simulate);
        }
    }

    /** An energy buffer of a machine. */
    public record EnergyChannel(IEnergyStorage storage) implements IValueChannel {
        @Override
        public long stored() {
            return storage.getEnergyStored();
        }

        @Override
        public long extract(long amount, boolean simulate) {
            return storage.extractEnergy((int) Math.min(amount, Integer.MAX_VALUE), simulate);
        }

        @Override
        public long insert(long amount, boolean simulate) {
            return storage.receiveEnergy((int) Math.min(amount, Integer.MAX_VALUE), simulate);
        }
    }

    /** What a machine's upgrades do to every recipe it runs. */
    public record RecipeModifiers(int maxParallel, float timeScale, float valueScale) {
        public static final RecipeModifiers NONE = new RecipeModifiers(1, 1f, 1f);

        public RecipeModifiers {
            maxParallel = Math.max(1, maxParallel);
            timeScale = Math.max(0.05f, timeScale);
            valueScale = Math.max(0f, valueScale);
        }
    }

    /** Everything one phase moves: the rolls are resolved and the modifiers already applied. */
    private record TickPlan(RecipeMaterials inputs, List<ItemStack> itemResults, List<FluidStack> fluidResults,
                            long energyIn, long chrononIn, long energyOut, long chrononOut) {
    }
}
