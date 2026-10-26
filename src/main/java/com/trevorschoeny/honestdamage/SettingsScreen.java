package com.trevorschoeny.honestdamage;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** YACL settings screen, opened from ModMenu's mods list. */
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
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Play a ding when damage goes down but a heart doesn't move"))
                                .description(OptionDescription.of(Component.literal(
                                        "A small ding when a hit takes some health but not enough to move a "
                                                + "heart. Off: that hit stays silent.")))
                                .binding(true, () -> Settings.ding, v -> Settings.ding = v)
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Component.literal("Ding sound"))
                                .description(OptionDescription.of(Component.literal(
                                        "The sound ID the ding plays, as in /playsound, e.g. "
                                                + "minecraft:block.note_block.bell. Blank or unknown: the iron armor clank.")))
                                .binding(Settings.DEFAULT_SOUND, () -> Settings.sound, v -> Settings.sound = v)
                                .controller(StringControllerBuilder::create)
                                .build())
                        .build())
                .save(Settings::save)
                .build()
                .generateScreen(parent);
    }
}
