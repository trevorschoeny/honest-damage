package com.trevorschoeny.honestdamage;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** YACL settings screen, opened from ModMenu's mods list. One tab, one toggle per feature. */
public final class SettingsScreen implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SettingsScreen::create;
    }

    public static Screen create(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Honest Damage"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Features"))
                        .option(toggle("Honest hurt cues",
                                "The hurt sound, red flash and screen shake play only when your heart bar "
                                        + "visibly drops. Off: every hit plays them, as in vanilla.",
                                () -> Settings.silencing, v -> Settings.silencing = v))
                        .option(toggle("Ding",
                                "A small ding when a hit takes some health but not enough to move a heart, "
                                        + "so you know it landed. A hit that takes nothing stays silent.",
                                () -> Settings.ding, v -> Settings.ding = v))
                        .build())
                .save(Settings::save)
                .build()
                .generateScreen(parent);
    }

    private static Option<Boolean> toggle(String name, String description, Supplier<Boolean> get, Consumer<Boolean> set) {
        return Option.<Boolean>createBuilder()
                .name(Component.literal(name))
                .description(OptionDescription.of(Component.literal(description)))
                .binding(true, get, set)
                .controller(BooleanControllerBuilder::create)
                .build();
    }
}
