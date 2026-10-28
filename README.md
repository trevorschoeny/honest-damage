# Honest Damage

Honest Damage makes the hurt effects tell the truth. In vanilla, every hit plays the hurt sound, flashes the screen red, and shakes the camera, even when your armor or an enchantment soaked it up and your hearts didn't move. With this mod, those effects only play when a heart actually goes down. A hit that takes a sliver of health without moving a heart plays a quiet iron armor clank instead, so you still know something hit you.

It's client-side, so it works on any server.

### Features

#### Hurt effects only when a heart moves

- The hurt sound, the red flash, and the screen shake wait until your hearts show whether the hit cost you anything.
- If a heart or half heart goes down, red or absorption, they play as usual.
- If none does, they don't play.

#### A clank for small hits

- A hit that takes some health but doesn't move a heart plays an iron armor clank.
- You can turn it off, or set it to any sound in the game.

### Requirements

- [Fabric API](https://modrinth.com/mod/fabric-api), which the bundled settings screen needs.

[YACL](https://modrinth.com/mod/yacl) comes bundled inside the jar, so there's nothing extra to install for it.

### Pairs well with

- [Mod Menu](https://modrinth.com/mod/modmenu), which opens the settings screen from the mod list.

### Compatibility

It runs on Minecraft 26.3 with Fabric. Install it on your client. It isn't needed on the server.

### How to use

There's nothing to set up. Play as usual.

- When a hit lands, the hurt sound, red flash, and screen shake are held for up to a fifth of a second while the game updates your health.
- If your red hearts or absorption hearts drop by at least half a heart, the effects play.
- If they don't drop, the effects are skipped. If the hit still took some health, the clank plays.
- Healing in that moment doesn't hide a hit, so a hit that regeneration heals right back still clanks.
- A hit that takes nothing at all stays silent. A shield block works as it does in vanilla.
- Health, armor, knockback, and death are untouched. Only the effects change.

#### Settings

Open the settings from Mod Menu. They're saved in `config/honestdamage/config.json`.

| Setting | Default |
| --- | --- |
| Play a ding when damage goes down but a heart doesn't move | On |
| Ding sound | `minecraft:item.armor.equip_iron` |

- Ding sound takes any sound ID, as in `/playsound`, such as `minecraft:block.note_block.bell`. A blank or unknown ID plays the iron armor clank.

### License

MIT.
