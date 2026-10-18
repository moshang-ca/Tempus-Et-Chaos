package org.moshang.tempusetchaos.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.client.gui.AbstractUiWindow;
import org.moshang.tempusetchaos.client.gui.ContainerWindowScreen;
import org.moshang.tempusetchaos.client.gui.IUiWindow;
import org.moshang.tempusetchaos.client.gui.UiTextures;
import org.moshang.tempusetchaos.client.gui.anim.*;
import org.moshang.tempusetchaos.client.gui.element.*;
import org.moshang.tempusetchaos.client.gui.window.SimpleContainerWindow;
import org.moshang.tempusetchaos.menu.MenuAccelerator;
import org.moshang.tempusetchaos.network.ChrononNetworkSyncPayload;
import org.moshang.tempusetchaos.network.MatchSetModePayload;
import org.moshang.tempusetchaos.network.MatchSetModifyPayload;
import org.moshang.tempusetchaos.network.MultiplierModifyPayload;
import org.moshang.tempusetchaos.util.MatchSet;
import org.moshang.tempusetchaos.util.TECLocalizations;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ScreenAccelerator extends ContainerWindowScreen<MenuAccelerator> {
    public ScreenAccelerator(MenuAccelerator menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    private static boolean isMatchable(ItemStack stack) {
        return stack.getItem() instanceof BlockItem || stack.getItem() instanceof SpawnEggItem;
    }

    @Override
    protected void populate() {
        windows.open(new MainWindow());
    }

    private class MainWindow extends SimpleContainerWindow<MenuAccelerator> {
        private static final ResourceLocation DISCLOSURE_BUTTON = UiTextures.ui("button_disclosure.png");

        private static final Cue SPIN = new Cue(0, 150, Easing.EASE_IN_EXPO);
        private static final Cue OPEN = Cue.startingAt(SPIN, 100, 200, Easing.EASE_IN_EXPO);

        private MatchSetInfoWindow infoWindow;

        public MainWindow() {
            super(ScreenAccelerator.this.menu, 176, 166, UiTextures.BASE_INVENTORY, 176, 166);
            elements.add(new ResourceGaugeElement(13, 12, 10, 60, UiTextures.CHANNEL,
                    () -> ChrononNetworkSyncPayload.ClientCache.getRatio(menu.getUuid()), -1, UiTextures.sprite("chronon"), this::getTooltip))
                    .add(new InputFieldElement(36, 12, 49, () -> String.valueOf(menu.getMultiplier()),
                            s -> {
                                int multiplier = Integer.parseInt(s);
                                menu.getBlockEntity().setAccelerateMultiplier(multiplier);
                                PacketDistributor.sendToServer(new MultiplierModifyPayload(menu.getBlockPos(), multiplier));
                            }).filter(s -> s.matches("\\d{0,9}")))
                    .add(new TextElement(36, 33, () -> Component.translatable(TECLocalizations.CHRONON_RATE,
                            TECLocalizations.formatCompact(-menu.getBlockEntity().getConsumed(), false, "CH")), 0xFFEF0101))
                    .add(new PhantomAreaElement(93, 12, 3, 3, this::matchSetStack, this::modifyMatchSet)
                            .filter(ScreenAccelerator::isMatchable).canOverflow(true))
                    .add(new TextureButtonElement(93, 67, 54, 7, false, DISCLOSURE_BUTTON, this::openInfo));

            for (int i = 0; i < 4; ++i) {
                elements.add(new SlotElement(151, 8 + i * 18, () -> ItemStack.EMPTY,
                        () -> List.of(Component.literal("Not implement yet"))).texture(UiTextures.UPGRADE_SLOT));
            }


            anims().add(Anims.timeline(AnimEvent.ADDED, SPIN, SPIN.track(0, 1), AnimProps.Writer.SCALE_X))
                    .add(Anims.timeline(AnimEvent.ADDED, OPEN, OPEN.track(0.01f, 1), AnimProps.Writer.SCALE_Y));
        }

        @Override
        protected void onChildForgotten(@NotNull IUiWindow child) {
            if (child == infoWindow) infoWindow = null;
        }

        private void openInfo() {
            if (infoWindow != null) return;
            MatchSetInfoWindow window = new MatchSetInfoWindow(this, 120, 156, this::matchSetEntries,
                    entry -> send(entry, 1), entry -> send(entry, 0));
            window.canDrag(true);
            openChild(infoWindow = window);
        }

        private List<MatchSet.Entry> matchSetEntries() {
            return menu.getBlockEntity().getMatchSet().ordered();
        }

        private List<Component> getTooltip() {
            List<Component> tooltip = new ArrayList<>();
            ChrononNetworkSyncPayload.ClientCache cache = ChrononNetworkSyncPayload.ClientCache.get(menu.getUuid());
            tooltip.add(Component.translatable(TECLocalizations.CHRONON, TECLocalizations.formatCompact(cache.stored(), false, "CH"),
                    TECLocalizations.formatCompact(cache.capacity(), false, "CH")));
            return tooltip;
        }

        private ItemStack matchSetStack(int index) {
            List<MatchSet.Entry> entries = matchSetEntries().stream()
                    .filter(entry -> !entry.isTag())
                    .filter(entry -> !(entry.kind() == MatchSet.Entry.Kind.ENTITY))
                    .toList();
            return index < entries.size() ? MatchSet.Entry.toItemStack(entries.get(index)) : ItemStack.EMPTY;
        }

        private void modifyMatchSet(ItemStack stack, int index) {
            List<MatchSet.Entry> entries = matchSetEntries().stream()
                    .filter(entry -> !entry.isTag())
                    .filter(entry -> !(entry.kind() == MatchSet.Entry.Kind.ENTITY))
                    .toList();
            boolean cell = index >= 0 && index < entries.size();
            if (stack.isEmpty()) {
                if (cell) send(entries.get(index), 0);
                return;
            }
            if (!isMatchable(stack)) return;
            if (cell) send(entries.get(index), 0);
            send(MatchSet.Entry.fromItemStack(stack), 1);
        }

        private void send(MatchSet.Entry entry, int modifier) {
            PacketDistributor.sendToServer(new MatchSetModifyPayload(menu.getBlockPos(), modifier, entry));
        }
    }

    private class MatchSetInfoWindow extends AbstractUiWindow {
        private static final int ROW_HEIGHT = 18;
        private static final int PADDING = 4;
        private static final int HEADER = 34;
        private static final int BUTTON_WIDTH = 24;
        private static final int BUTTON_HEIGHT = 12;
        private static final List<MatchSet.Entry.Kind> KINDS = List.of(MatchSet.Entry.Kind.BLOCK, MatchSet.Entry.Kind.ENTITY);

        private final Supplier<List<MatchSet.Entry>> entries;
        private final Consumer<MatchSet.Entry> onAdd;
        private final Consumer<MatchSet.Entry> onRemove;
        private final ToggleElement<MatchSet.Entry.Kind> kinds;
        private final InputFieldElement search;
        private final ListElement list;

        private List<MatchSet.Entry> shown = List.of();
        private List<ItemStack> icons = List.of();
        private List<Component> labels = List.of();
        private List<MatchSet.Entry> source = List.of();
        private MatchSet.Entry.Kind kind = KINDS.getFirst();
        private String filter = "";
        @Nullable
        private AddEntryWindow addWindow;

        public MatchSetInfoWindow(IUiWindow parent, int width, int height, Supplier<List<MatchSet.Entry>> entries,
                                  Consumer<MatchSet.Entry> onAdd, Consumer<MatchSet.Entry> onRemove) {
            super(AbstractUiWindow.centered(parent.getX(), parent.getWidth(), width),
                    AbstractUiWindow.centered(parent.getY(), parent.getHeight(), height), width, height);
            this.parent = parent;
            this.entries = entries;
            this.onAdd = onAdd;
            this.onRemove = onRemove;

            search = new InputFieldElement(PADDING, PADDING + 16, width - PADDING * 2, 14, "");
            list = new ListElement(PADDING, PADDING + HEADER, width - PADDING * 2, height - PADDING * 2 - HEADER, ROW_HEIGHT)
                    .size(() -> shown.size())
                    .icons(this::icon)
                    .labels(this::label);
            kinds = new ToggleElement<>(PADDING, PADDING, width - PADDING * 2, 12, KINDS,
                    kind -> Component.literal(kind.name()), kind -> list.getScroll().reset());
            list.controls().add(new TextureButtonElement(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, UiTextures.BUTTON, true,
                        () -> Component.literal("+"), this::openAdd))
                    .add(new TextureButtonElement(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, UiTextures.BUTTON, true,
                        () -> Component.literal("-"), this::remove)
                    .enabled(() -> list.selected() >= 0))
                    .add(new InplaceToggleElement<>(0, 0, 18, 18, List.of(true, false), this::changeMatchMode)
                            .addOptionBg(UiTextures.BLACKLIST).addOptionBg(UiTextures.WHITELIST).showing(() -> menu.getBlockEntity().isBlacklistMode()));
            elements.add(kinds).add(search).add(list);
        }

        @Override
        public void updateAnimation() {
            rebuildShown();
            super.updateAnimation();
        }

        private void rebuildShown() {
            String text = search.getValue().toLowerCase(Locale.ROOT);
            MatchSet.Entry.Kind selected = kinds.getSelected();
            List<MatchSet.Entry> source = this.entries.get();
            if (source == this.source && selected == kind && text.equals(filter)) return;
            this.source = source;
            kind = selected;
            filter = text;

            List<MatchSet.Entry> next = source.stream()
                    .filter(entry -> entry.kind() == selected)
                    .filter(entry -> text.isEmpty() || entry.id().toString().contains(text))
                    .toList();
            if (!next.equals(shown)) list.selected(-1);
            shown = next;
            icons = next.stream().map(MatchSet.Entry::toItemStack).toList();
            labels = next.stream().map(MatchSetInfoWindow::label).toList();
        }

        private void remove() {
            int index = list.selected();
            if (index >= 0 && index < shown.size()) onRemove.accept(shown.get(index));
        }

        @Override
        protected void onChildForgotten(@NotNull IUiWindow child) {
            if (child == addWindow) addWindow = null;
        }

        private void openAdd() {
            if (addWindow != null) return;
            AddEntryWindow window = new AddEntryWindow(this, 100, 140, entries, onAdd);
            window.canDrag(true);
            openChild(addWindow = window);
        }

        private void changeMatchMode(boolean b) {
            PacketDistributor.sendToServer(new MatchSetModePayload(menu.getBlockPos(), b));
        }

        private ItemStack icon(int index) {
            return index < icons.size() ? icons.get(index) : ItemStack.EMPTY;
        }

        private Component label(int index) {
            return index < labels.size() ? labels.get(index) : Component.empty();
        }

        private static Component label(MatchSet.Entry entry) {
            return Component.literal((entry.isTag() ? "#" : "") + entry.id());
        }

        @Override
        protected void renderContent(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            UiTextures.blitNineSliced(graphics, UiTextures.PANEL, 0, 0, getWidth(), getHeight(), false);
        }
    }

    private static class AddEntryWindow extends AbstractUiWindow {
        private static final int ROW_HEIGHT = 12;
        private static final int PADDING = 4;
        private static final int BUTTON_WIDTH = 24;
        private static final int BUTTON_HEIGHT = 12;
        private static final List<MatchSet.Entry.Kind> KINDS = List.of(MatchSet.Entry.Kind.BLOCK, MatchSet.Entry.Kind.ENTITY);

        private final Supplier<List<MatchSet.Entry>> entries;
        private final Consumer<MatchSet.Entry> onAdd;
        private final ToggleElement<MatchSet.Entry.Kind> kinds;
        private final InputFieldElement filter;
        private final ListElement candidates;

        private List<ResourceLocation> shown = List.of();
        private List<Component> labels = List.of();
        private List<MatchSet.Entry> taken = List.of();
        private MatchSet.Entry.Kind kind = KINDS.getFirst();
        private String filterText = "";
        private ItemStack sampleStack = ItemStack.EMPTY;
        private ItemStack sample = ItemStack.EMPTY;

        public AddEntryWindow(IUiWindow parent, int width, int height,
                              Supplier<List<MatchSet.Entry>> entries, Consumer<MatchSet.Entry> onAdd) {
            super(AbstractUiWindow.centered(parent.getX(), parent.getWidth(), width),
                    AbstractUiWindow.centered(parent.getY(), parent.getHeight(), height), width, height);
            this.parent = parent;
            this.entries = entries;
            this.onAdd = onAdd;

            filter = new InputFieldElement(PADDING, PADDING + 16, width - PADDING * 2 - 20, 14, "");
            candidates = new ListElement(PADDING, PADDING + 36, width - PADDING * 2, height - PADDING * 2 - 36, ROW_HEIGHT)
                    .size(() -> shown.size())
                    .labels(this::label);
            kinds = new ToggleElement<>(PADDING, PADDING, width - PADDING * 2, 12, KINDS,
                    kind -> Component.literal(kind.name()), kind -> candidates.getScroll().reset());
            candidates.controls().add(new TextureButtonElement(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, UiTextures.BUTTON, true,
                    () -> Component.literal("+"), this::add));
            candidates.controls().add(new TextureButtonElement(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, UiTextures.BUTTON, true,
                    () -> Component.literal("X"), this::closeSelf));
            elements.add(kinds).add(filter).add(candidates)
                    .add(new PhantomSlotElement(width - PADDING - 16, PADDING + 15, () -> sampleStack, stack -> sampleStack = stack));
        }

        @Override
        public void updateAnimation() {
            rebuildShown();
            super.updateAnimation();
        }

        private void rebuildShown() {
            if (!sampleStack.isEmpty()) kinds.setSelected(MatchSet.Entry.Kind.fromItemStack(sampleStack));
            String text = filter.getValue().toLowerCase(Locale.ROOT);
            MatchSet.Entry.Kind selected = kinds.getSelected();
            List<MatchSet.Entry> source = this.entries.get();
            if (source == taken && selected == kind && text.equals(filterText) && sampleStack == sample) return;
            candidates.selected(-1);
            taken = source;
            kind = selected;
            filterText = text;
            sample = sampleStack;

            shown = switch (selected) {
                case BLOCK -> {
                    Block block = sampleStack.getItem() instanceof BlockItem item ? item.getBlock() : null;
                    yield BuiltInRegistries.BLOCK.getTagNames()
                            .filter(tag -> tag.location().toString().contains(text))
                            .filter(tag -> block == null || block.defaultBlockState().is(tag))
                            .map(TagKey::location)
                            .filter(id -> !source.contains(MatchSet.Entry.blockTag(id)))
                            .toList();
                }
                case ENTITY -> {
                    EntityType<?> type = sampleStack.getItem() instanceof SpawnEggItem egg ? egg.getType(sampleStack) : null;
                    yield BuiltInRegistries.ENTITY_TYPE.getTagNames()
                            .filter(tag -> tag.location().toString().contains(text))
                            .filter(tag -> type == null || type.is(tag))
                            .map(TagKey::location)
                            .filter(id -> !source.contains(MatchSet.Entry.entityTag(id)))
                            .toList();
                }
                default -> List.of();
            };
            labels = shown.stream().map(id -> (Component) Component.literal("#" + id)).toList();
        }

        private void add() {
            int index = candidates.selected();
            if (index < 0 || index >= shown.size()) {
                if (sampleStack.isEmpty()) return;
                MatchSet.Entry entry = MatchSet.Entry.fromItemStack(sampleStack);
                // If the player want to add a specific item, then we should consume it as a feedback
                sampleStack = ItemStack.EMPTY;
                onAdd.accept(entry);
                return;
            }
            ResourceLocation id = shown.get(index);
            onAdd.accept(kinds.getSelected() == MatchSet.Entry.Kind.BLOCK
                    ? MatchSet.Entry.blockTag(id)
                    : MatchSet.Entry.entityTag(id));
        }

        private Component label(int index) {
            return index < labels.size() ? labels.get(index) : Component.empty();
        }

        @Override
        protected void renderContent(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            UiTextures.blitNineSliced(graphics, UiTextures.PANEL, 0, 0, getWidth(), getHeight(), false);
        }
    }
}
