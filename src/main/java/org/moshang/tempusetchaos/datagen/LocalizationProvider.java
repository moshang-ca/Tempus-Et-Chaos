package org.moshang.tempusetchaos.datagen;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.client.gui.annotation.Localization;
import org.moshang.tempusetchaos.client.gui.annotation.RegistryLocalization;
import org.moshang.tempusetchaos.client.gui.annotation.Translation;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@Slf4j
public class LocalizationProvider extends LanguageProvider {
    protected final String locale;

    private final Class<?>[] langClazz;

    public LocalizationProvider(PackOutput output, String modid, String locale, Class<?>... clazz) {
        super(output, modid, locale);
        this.locale = locale;
        this.langClazz = clazz;
    }

    @Override
    protected void addTranslations() {
        for (Class<?> clazz : langClazz) {
            scanClass(clazz);
        }
    }

    private void scanClass(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;

            Localization loc = field.getAnnotation(Localization.class);
            if (loc != null && field.getType() == String.class) {
                String text = pick(loc.translations());
                if (text == null) continue;
                try {
                    String key = (String) field.get(null);
                    if (key != null) add(key, text);
                } catch (IllegalAccessException e) {
                    log.error("Can not access {}", field, e);
                }
                continue;
            }

            RegistryLocalization regLoc = field.getAnnotation(RegistryLocalization.class);
            if (regLoc != null && DeferredHolder.class.isAssignableFrom(field.getType())) {
                String text = pick(regLoc.translations());
                if (text == null) continue;
                try {
                    DeferredHolder<?, ?> holder = (DeferredHolder<?, ?>) field.get(null);
                    String key = getRegistryKey(holder.get());
                    if (key != null) add(key, text);
                } catch (IllegalAccessException e) {
                    log.error("Can not access {}", field, e);
                }
            }
        }
    }

    @Nullable
    private String pick(Translation[] translations) {
        for (Translation translation : translations) {
            if (translation.locale().equals(locale)) return translation.text();
        }
        log.warn("Found no translation for {}", locale);
        return null;
    }

    @Nullable
    private String getRegistryKey(Object value) {
        return switch (value) {
            case Block block -> block.getDescriptionId();
            case Item item -> item.getDescriptionId();
            case Fluid fluid -> fluid.getFluidType().getDescriptionId();
            default -> {
                log.warn("Unsupported value for {}", value);
                yield null;
            }
        };
    }
}
