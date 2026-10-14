package org.moshang.tempusetchaos.api.storage;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class NotifiedFluidHandler implements IFluidHandler {
    @Accessors(chain = true) @Setter
    private Runnable onContentsChanged = () -> {};
    private final FluidTank[] tanks;

    public NotifiedFluidHandler(int size, int capacity) {
        this.tanks = new FluidTank[size];
        for (int i = 0; i < size; i++) {
            this.tanks[i] = new FluidTank(capacity) {
                @Override
                protected void onContentsChanged() {
                    onContentsChanged.run();
                }
            };
        }
    }

    public NotifiedFluidHandler setFilter(int tank, Predicate<FluidStack> filter) {
        tanks[tank].setValidator(filter);
        return this;
    }

    public NotifiedFluidHandler setFilters(Predicate<FluidStack> filter) {
        for (FluidTank tank : tanks) {
            tank.setValidator(filter);
        }
        return this;
    }

    public List<FluidStack> getContents() {
        return Arrays.stream(tanks).map(FluidTank::getFluid).toList();
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) return FluidStack.EMPTY;

        FluidStack first = FluidStack.EMPTY;
        for (IFluidTank tank : tanks) {
            if (!tank.getFluid().isEmpty()) {
                first = tank.getFluid();
                break;
            }
        }
        if (first.isEmpty()) return FluidStack.EMPTY;

        int totalDrained = 0;
        //noinspection DuplicatedCode
        for (IFluidTank tank : tanks) {
            if (totalDrained >= maxDrain) break;

            FluidStack content = tank.getFluid();
            if (content.isEmpty()) continue;
            if (!FluidStack.isSameFluidSameComponents(content, first)) continue;

            int toDrain = Math.min(maxDrain - totalDrained, content.getAmount());
            FluidStack drained = tank.drain(toDrain, action);
            totalDrained += drained.getAmount();
        }

        if (totalDrained == 0) return FluidStack.EMPTY;
        return first.copyWithAmount(totalDrained);
    }

    @Override
    public int getTanks() {
        return tanks.length;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tanks[tank].getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return tanks[tank].getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tanks[tank].isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;

        int filled = 0;
        int total = resource.getAmount();
        for (IFluidTank tank : tanks) {
            if (filled >= total) break;

            FluidStack content = tank.getFluid();
            if (!content.isEmpty() && !FluidStack.isSameFluidSameComponents(content, resource)) continue;

            int remaining = total - filled;
            int filled1 = tank.fill(resource.copyWithAmount(remaining), action);
            filled += filled1;
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;

        int totalDrained = 0;
        int maxDrain = resource.getAmount();
        //noinspection DuplicatedCode
        for (IFluidTank tank : tanks) {
            if (totalDrained >= maxDrain) break;

            FluidStack content = tank.getFluid();
            if (content.isEmpty()) continue;
            if (!FluidStack.isSameFluidSameComponents(content, resource)) continue;

            int toDrain = Math.min(maxDrain - totalDrained, content.getAmount());
            FluidStack drained = tank.drain(toDrain, action);
            totalDrained += drained.getAmount();
        }

        return totalDrained == 0 ? FluidStack.EMPTY : resource.copyWithAmount(totalDrained);
    }
}
