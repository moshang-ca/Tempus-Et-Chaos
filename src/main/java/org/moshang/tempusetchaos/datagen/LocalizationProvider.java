package org.moshang.tempusetchaos.datagen;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.annotation.Localization;
import org.moshang.tempusetchaos.annotation.RegistryLocalization;
import org.moshang.tempusetchaos.annotation.Translation;

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
            scanNoRegistries(clazz);
            scanRegistries(clazz);
        }
    }

    private void scanRegistries(Class<?> clazz) {
        RegistryLocalization classAnn = clazz.getAnnotation(RegistryLocalization.class);

        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (!DeferredHolder.class.isAssignableFrom(field.getType())) continue;

            RegistryLocalization fieldAnn = field.getAnnotation(RegistryLocalization.class);
            if (fieldAnn != null && fieldAnn.skip()) continue;
            if (fieldAnn == null && classAnn == null) continue;

            Translation[] translations = fieldAnn != null ? fieldAnn.translations() : new Translation[0];
            try {
                DeferredHolder<?, ?> holder = (DeferredHolder<?, ?>) field.get(null);
                String text = pick(translations, holder);
                if (text == null) continue;

                String key = getRegistryKey(holder.get());
                if (key != null) {
                    add(key, text);
                } else {
                    log.warn("Cannot derive key for {}", field);
                }
            } catch (IllegalAccessException e) {
                log.error("Cannot access {}", field, e);
            }
        }
    }

    private void scanNoRegistries(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (field.getType() != String.class) continue;

            Localization loc = field.getAnnotation(Localization.class);
            if (loc == null) continue;
            String text = pick(loc.translations(), null);
            if (text == null) continue;

            try {
                String key = (String) field.get(null);
                if (key != null && !key.isEmpty()) add(key, text);
                else log.warn("Empty key on {}", field);
            } catch (IllegalAccessException e) {
                log.error("Cannot access {}", field, e);
            }
        }
    }

    @Nullable
    private String pick(Translation[] translations, @Nullable DeferredHolder<?, ?> holder) {
        for (Translation translation : translations) {
            if (translation.locale().equals(locale)) return translation.text();
        }
        if (holder != null && locale.equals("en_us")) return toDisplayName(holder);
        log.warn("Found no translation for {}", locale);
        return null;
    }

    @Nullable
    private String getRegistryKey(Object value) {
        return switch (value) {
            case Block block -> block.getDescriptionId();
            case Item item -> item.getDescriptionId();
            case Fluid fluid -> fluid.getFluidType().getDescriptionId();
            case FluidType fluidType -> fluidType.getDescriptionId();
            default -> {
                log.warn("Unsupported value for {}", value);
                yield null;
            }
        };
    }

    protected static <R, T extends R> String toDisplayName(DeferredHolder<R, T> registerHolder) {
        String[] words = registerHolder.getRegisteredName().split(":")[1].split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            sb.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1))
                    .append(" ");
        }
        return sb.toString().trim();
    }
}
