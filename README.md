# Tiered More

![icon.png](common%2Fsrc%2Fmain%2Fresources%2Fassets%2Ftiered%2Ficon.png)

### Usage:
- Head [here](https://github.com/Globox1997/tiered/blob/1.21/README.md) since most of everything is the same.

### New features

- Modifiers now work on accessories
```
{
    "id": "tiered:hasty",
    "verifiers": [
        {
            "tag": "tclayer:all_trinket_items"
        }
    ],
    "weight": 50,
    "style": {
        "color": "blue"
    },
    "attributes": [
        {
            "type": "minecraft:player.block_break_speed",
            "modifier": {
                "name": "tiered:hasty",
                "operation": "ADD_MULTIPLIED_TOTAL",
                "amount": 0.05
            },
            "optional_accessories_slots": [
                "anklet",
                "any",
                "back",
                "belt",
                "cape",
                "charm",
                "face",
                "hand",
                "hat",
                "necklace",
                "ring",
                "shoes",
                "trinket_group_misc-quiver",
                "trinket_group_spell-book",
                "trinket_group_spell-quiver",
                "trinket_group_spell-scroll",
                "trinket_group_spell-trinket",
                "wrist"
            ]

        }
    ]
}
```

- Optional field "cursed": "true" / "false"
Cursed Modifiers can only be obtained through found loot. You are unable to reforge the Cursed Item unless you use the Blessed Scroll (v1.0.9)
```
{
    "id": "tiered:sadistic",
    "verifiers": [
        {
            "tag": "tclayer:all_trinket_items"
        }
    ],
    "cursed": "true",
    "weight": 25,
    "style": {
        "color": "red"
    },
    "attributes": [
        {
            "type": "minecraft:generic.attack_damage",
            "modifier": {
                "name": "tiered:sadistic",
                "operation": "ADD_MULTIPLIED_TOTAL",
                "amount": 0.025
            },
            "optional_accessories_slots": [
                "anklet",
                "any",
                "back",
                "belt",
                "cape",
                "charm",
                "face",
                "hand",
                "hat",
                "necklace",
                "ring",
                "shoes",
                "trinket_group_misc-quiver",
                "trinket_group_spell-book",
                "trinket_group_spell-quiver",
                "trinket_group_spell-scroll",
                "trinket_group_spell-trinket",
                "wrist"
            ]

        },
        {
            "type": "minecraft:generic.armor",
            "modifier": {
                "name": "tiered:sadistic",
                "operation": "ADD_MULTIPLIED_TOTAL",
                "amount": -0.035
            },
            "optional_accessories_slots": [
                "anklet",
                "any",
                "back",
                "belt",
                "cape",
                "charm",
                "face",
                "hand",
                "hat",
                "necklace",
                "ring",
                "shoes",
                "trinket_group_misc-quiver",
                "trinket_group_spell-book",
                "trinket_group_spell-quiver",
                "trinket_group_spell-scroll",
                "trinket_group_spell-trinket",
                "wrist"
            ]

        }
    ]
}
```
Add spells to modifiers 
```
{
  "id": "tiered:spell_armor",
  "verifiers": [
    {
      "tag": "minecraft:chest_armor"
    }
  ],
  "weight": 100,
  "style": {
    "color": "aqua"
  },
  "attributes": [
    {
      "type": "generic.armor",
      "modifier": {
        "name": "tiered:spell_armor",
        "operation": "ADD_VALUE",
        "amount": 2
      },
      "optional_equipment_slots": [
        "CHEST"
      ]
    }
  ],
  "spells": [
    {
      "spell_id": "wizards:fireball",
      "optional_equipment_slots": [
        "CHEST"
      ]
    }
  ]
}
```
reforge_items/*.json are now able to use tags
```
{
    "items": [
        "#tclayer:all_trinket_items"
    ],
    "base": [
        "minecraft:lapis_lazuli"
    ]
}
```
item_attributes/*.json now has an optional exclude item
```
{
    "id": "tiered:hasty",
    "verifiers": [
        {
            "tag": "tclayer:all_trinket_items"
        }
    ],
    "excludes": [
        {
            "tag": "icarus:wings"
        }
    ],
    "weight": 50,
    "style": {
        "color": "blue"
    },
    "attributes": [
        {
            "type": "minecraft:player.block_break_speed",
            "modifier": {
                "name": "tiered:hasty",
                "operation": "ADD_MULTIPLIED_TOTAL",
                "amount": 0.05
            },
            "optional_accessories_slots": [
                "anklet",
                "any",
                "back",
                "belt",
                "cape",
                "charm",
                "face",
                "hand",
                "hat",
                "necklace",
                "ring",
                "shoes",
                "trinket_group_misc-quiver",
                "trinket_group_spell-book",
                "trinket_group_spell-quiver",
                "trinket_group_spell-scroll",
                "wrist"
            ]

        }
    ]
}
```
Exclude items or tags via:
```
"excludes": [
    {
        "tag": "c:elytra"
    }
]
```
or
```
"excludes": [
    {
        "id": "minecraft:elytra"
    }
]
```

reforge_material/*.json - datapack-defined reforging materials

Any item placed in the reforge addition slot can be given custom behavior that biases the modifier roll. A material targets one item (`"item"`) or an item tag (`"item": "#namespace:tag"`). All effect fields are optional and can be combined. Make sure the item is also in the `#tiered:reforge_addition` tag so it can be inserted.

Fields:
- `groups` — hard filter. Only roll modifiers whose group (the first part of the modifier id, e.g. `legendary_armor_1` → `legendary`) is in this list.
- `group_weight_multipliers` — soft bias. Multiply the roll weight of modifiers in the named group. `3.0` means 3× as likely relative to its base weight; `1.0` is no change.
- `rarity_boost` — 0.0 to 1.0. Biases the roll toward rarer (lower-weight) tiers by suppressing common modifiers. Does **not** guarantee a rarity (use `guaranteed_min_rarity` for that).
- `guaranteed_min_rarity` — floor. The result is at least this rarity tier when the item has a qualifying modifier.
- `max_rarity` — ceiling. Modifiers above this rarity are removed from the pool before rolling.
- `compatible` — item ids or `#tags` the material may be used on. Empty/absent means any item is allowed.
- `incompatible` — item ids or `#tags` the material may NOT be used on (checked first; overrides `compatible`).
- `color` — tooltip header color (a vanilla `Formatting` name).
- `description` — info lines shown on the material's tooltip (when expanded with Alt). Each entry is a
  translation key, falling back to literal text if the key isn't found, so you can localize or inline.
- `skip_reforge` — when `true`, applying this material does NOT reroll the item's tier/modifiers; it only
  runs the material's effects and grants its imprints. Used by imprint-granting runes.
- `skip_base_item` — when `true`, no item is required in the base (slot 0) — the material acts standalone
  (e.g. extract/forget runes). May still reroll unless `skip_reforge` is also set.
- `effects` — list of effect ids (see Effects below) always run on reforge, e.g. `["tiered:mend"]`.
- `effect_pool` / `effect_pools` — roll one or more effects from a weighted pool at rune generation (see
  Rune content pools below). The rolled effects are stamped on the rune instance.
- `imprint_pool` / `imprint_pools` — roll imprints to grant from a weighted pool at rune generation.
- `behavior_pool` — roll a bias fragment (groups/rarity_boost/etc.) from a weighted pool at generation.
- `hidden_badges` — badge keys to suppress from the tooltip (e.g. `["effect_pool", "stabilize"]`).

Rarity order is `common, uncommon, rare, epic, legendary, unique`.

A rune that only rolls Epic/Legendary, weighted toward Legendary, never below Rare:
```
{
    "item": "tiered:rune_arcane",
    "color": "light_purple",
    "groups": ["epic", "legendary"],
    "group_weight_multipliers": {
        "legendary": 3.0
    },
    "guaranteed_min_rarity": "rare",
    "description": [
        "An arcane reforging rune."
    ]
}
```

A cheap material capped at low tiers, applied to a whole tag, never usable on trinkets:
```
{
    "item": "#c:gems",
    "color": "gray",
    "max_rarity": "uncommon",
    "incompatible": ["#tclayer:all_trinket_items"]
}
```

A neutral baseline material with no bias (rolls anything):
```
{
    "item": "minecraft:amethyst_shard",
    "color": "light_purple",
    "description": [
        "The standard reforging material.",
        "Rolls any modifier with no bias."
    ]
}
```

imprint/*.json — datapack-defined imprints

An imprint is a persistent enchant-like effect applied to an item via a reforge effect or rune. Imprints
occupy slots on the item (see imprint slots) and show as colored plates in the tooltip. Define them in
`data/tiered/imprint/<name>.json`.

Top-level fields:
- `id` — the imprint id, e.g. `tiered:swiftness`.
- `color` — plate color: a vanilla `Formatting` name (`GOLD`) or a `#RRGGBB` hex string.
- `name_key` / `line_key` — translation keys for the plate name and the tooltip line.
- `combine` — how two values of this imprint merge: `ADDITIVE` (sum), `HIGHEST` (keep the larger), `UNIQUE`.
- `multiplicity` — `SLOT` (each grant is its own slot; values sum) or `MERGE`.
- `reapply` — what happens when re-granted: `REPLACE`, `ACCUMULATE`, `HIGHEST`, `REJECT`.
- `group` / `groups` — pool group(s) an imprint belongs to, for `imprint_group`-driven rune pools (e.g.
  `movement`, `combat`, `defense`, `weapon`, `vitality`, `survival`).
- `active_when` / `inactive_when` — eligibility predicates (e.g. only while a sword is in the main hand). An
  `equipped_item` clause matches by `tags` (ALL must match), `any_tags` (ANY one matches, e.g. sword OR axe
  OR mace), or `items` (exact ids).
- `types` — one or more type components (see below). A single imprint may grant both an attribute and a
  behavioral bonus by listing multiple components.

Each entry in `types` declares:
- `type` — `attribute` or `behavioral`.
- `value` / `value_min` / `value_max` — the magnitude (fixed, or a roll range).
- `value_display` — how the value renders in the tooltip: `raw` (default; 1 decimal), `percent`
  (value × 100, for `%` lines), or `none` (no number).
- `max_stacks` — cap on how many copies contribute.
- For `attribute`: `attribute` (id), `operation` (`ADD_VALUE` / `MULTIPLY_BASE` / `MULTIPLY_TOTAL`),
  `any_worn`, `required_slots`, `optional_slots`, `accessories_slots`.
- For `behavioral`: `behavior` (a Java behavior id, e.g. `tiered:airborne_melee`) and `params`. Behaviors can
  trigger on melee, ranged (projectile) and magic damage, on kills, on a per-tick basis, and on equip/unequip.

Behavioral damage imprints classify a hit as MELEE (direct), RANGED (player projectile), or MAGIC. The
ranged/magic classification is data-overridable via two damage-type tags — add modded damage types to either:
- `tiered:counts_as_magic` (defaults to including `#c:is_magic`) — hits of these types trigger magic imprints.
- `tiered:counts_as_ranged` — hits of these types also count as ranged (in addition to actual projectiles).

A movement-speed imprint (attribute) and an airborne melee-damage imprint (behavioral):
```
{
  "id": "tiered:swiftness",
  "color": "#1ABC9C",
  "name_key": "imprint.tiered.swiftness.name",
  "line_key": "imprint.tiered.swiftness",
  "combine": "ADDITIVE", "multiplicity": "SLOT", "group": "movement",
  "types": [
    { "type": "attribute", "value_display": "percent", "value_min": 0.03, "value_max": 0.06,
      "max_stacks": 3, "attribute": "minecraft:generic.movement_speed", "operation": "MULTIPLY_BASE",
      "any_worn": true }
  ]
}
```

effect/*.json — datapack-defined reforge effects

An effect runs when a material is applied. Define them in `data/tiered/effect/<name>.json` and reference
by id from a material's `effects` / `effect_pool`.

- `id` — the effect id, e.g. `tiered:mend`.
- `type` — `repair`, `grant_imprint`, `forget`, `extract`, `stabilize`, `overcharge`, `grant_rune_slot`, or
  `custom`.
- `value` / `value_min` / `value_max` — type-specific magnitude (e.g. `repair` = fraction of max durability;
  `extract` = success chance; `grant_rune_slot` = slots to add).
- `imprint` — for `grant_imprint`, the imprint id to grant.
- `description` — translation key (or literal) shown in the codex and material tooltip.
- `applies_to` / `not_applies_to` — item gating for ANY effect type. Lists of item ids or `#tags`; the effect
  only runs on items matching `applies_to` (empty/absent = any) and never on `not_applies_to`. E.g. gate a
  slot-grant to `["#minecraft:enchantable/weapon", "#minecraft:enchantable/armor"]`.
- `params` — type-specific tuning (e.g. `overcharge`: `extra_cost_min`/`extra_cost_max`/`boost_per_cost`/
  `min_rarity_order`; `grant_rune_slot`: `max_total` per-item slot ceiling, `max_grants` one-time limit).

```
{ "id": "tiered:mend", "type": "repair", "value_min": 0.1, "value_max": 0.25 }
```

The `grant_rune_slot` effect adds rune slots to the target and is consumed without occupying a slot. Pair it
with a standalone rune material (`skip_reforge` + `skip_base_item`, `compatible` gated to weapons/armor).
Total slots are capped by the `maxImprintSlots` config (default 6). Example:
```
{ "id": "tiered:grant_rune_slot", "type": "grant_rune_slot", "value": 1,
  "params": { "max_total": 0, "max_grants": 0 } }   // max_grants:1 = one slot grant per item, ever
```

Rune content pools (rolled per item instance)

A material can declare pools that roll PER rune instance at generation (loot or first inventory tick), so
two runes of the same type can carry different content. Each pool supports `roll_min`/`roll_max` and, when
`roll_min` is 0, a `nothing_weight` (the implicit "rolled nothing" outcome, defaulting to the sum of
candidate weights).

- `imprint_pool` / `imprint_pools` — roll imprints to grant. Candidates can be listed explicitly (with
  `value_min`/`value_max`/`weight`) or pulled from a group via `imprint_group` (`"all"` for every imprint).
  `default_imprints` are always granted; `allow_duplicates` lets the same candidate roll more than once.
- `effect_pool` / `effect_pools` — roll effect ids (each candidate `{ "effect": id, "weight": n }`).
- `behavior_pool` — roll one bias fragment (any subset of `groups` / `group_weight_multipliers` /
  `rarity_boost` / `guaranteed_min_rarity` / `max_rarity`) from weighted candidates.

**Pool choices (OR).** The `*_pools` lists above all roll (AND). To instead pick ONE whole sub-pool by
weight, use `imprint_pool_choices` / `effect_pool_choices` / `behavior_pool_choices`. Each holds
`candidates: [{ "weight": n, "pool": { ...a normal pool... } }]`; exactly one candidate pool is chosen and
rolled, in addition to any always-on `*_pools`. The chosen sub-pool still runs its own `roll_min`/`roll_max`
and `nothing_weight`, so it may roll one thing, several, or nothing. Example — a rune that does EITHER an
extract OR grants Reinforced:
```
"effect_pool_choices": {
  "candidates": [
    { "weight": 2, "pool": { "roll_min": 1, "roll_max": 1, "candidates": [ { "effect": "tiered:extract" } ] } },
    { "weight": 1, "pool": { "roll_min": 1, "roll_max": 1, "candidates": [ { "effect": "tiered:grant_reinforced" } ] } }
  ]
}
```
Or a behavior choice between "rare+ with a boost" and "epic only":
```
"behavior_pool_choices": {
  "candidates": [
    { "weight": 1, "pool": { "roll_min": 1, "roll_max": 1, "candidates": [
        { "rarity_boost": 0.3, "guaranteed_min_rarity": "rare" } ] } },
    { "weight": 1, "pool": { "roll_min": 1, "roll_max": 1, "candidates": [
        { "max_rarity": "epic", "guaranteed_min_rarity": "epic" } ] } }
  ]
}
```
- `roll_scaling` (on an `imprint_pool`) — a list of rules that raise the roll based on the LOOT drop context,
  so tougher sources drop richer runes. Each rule: a `source` (`entity_max_health`, `entity_tag` with `tag`,
  or `dimension` with `dimension`), `per` (step size; `1` for the flag sources), `min_bonus_per`/`max_bonus_per`
  (+how many imprints roll) and/or `value_min_bonus_per`/`value_max_bonus_per` (+how strong each rolls), each
  with a matching `*_cap`. Only applies to loot-generated runes (a `/give`n rune uses base ranges).
  Value boosts are clamped to each imprint's own max. Example:
  ```
  "roll_scaling": [
    { "source": "entity_max_health", "per": 100, "min_bonus_per": 1, "min_bonus_cap": 3 },
    { "source": "entity_tag", "tag": "#c:bosses", "per": 1, "max_bonus_per": 1, "max_bonus_cap": 1 }
  ]
  ```

```
{
  "item": "tiered:rune_crimson",
  "color": "dark_red",
  "skip_reforge": true,
  "imprint_pools": [
    { "roll_min": 1, "roll_max": 2, "imprint_group": "weapon", "allow_duplicates": true },
    { "roll_min": 1, "roll_max": 2, "imprint_group": "combat", "allow_duplicates": true }
  ]
}
```

rune_injection/*.json — inject runes into existing loot tables

Adds rune drops to vanilla/modded loot tables without overwriting them. Define in
`data/tiered/rune_injection/<name>.json`. Targeting is by `tables` (exact ids) and/or `table_patterns`
(substring match). Optional context filters (all ANDed; each list ORs internally): `dimensions`,
`dimension_tags`, `biomes`, `biome_tags`, `structures`, `structure_tags`, `entities`, `entity_tags`.

Each `pools` entry is an independent roll: `items` (weighted candidates), `chance` (per-roll hit chance),
and `count_min`/`count_max`. Each rolled rune is an independent count-1 stack with its own rolled content
(a `count_max` of 4 means up to 4 independent draws, not 4 of one rune).

> Note: entity filters check the dying entity, so they only fire when the loot roll has an entity in
> context — a real mob death or `/loot give <target> kill @e[...]`, not `/loot give <target> loot <table>`.

```
{
  "entity_tags": ["#c:bosses"],
  "table_patterns": ["entities/"],
  "pools": [
    {
      "items": [
        { "item": "tiered:rune_amber", "weight": 3 },
        { "item": "tiered:rune_crimson", "weight": 2 }
      ],
      "chance": 0.6,
      "count_min": 1,
      "count_max": 2
    }
  ]
}
```

Bonus rune slots from loot — `tiered:grant_bonus_imprint_slots` loot function

A loot function that adds additive rune slots to the generated item (clamped to the `maxImprintSlots` config
and an optional per-function `max_total`). Place it on a loot table's item entry and gate it with the table's
own conditions (e.g. `location_check` for a structure or dimension) to grant extra slots to items found in
specific places. Additive — stacks with other slot sources up to the cap.
```
{ "function": "tiered:grant_bonus_imprint_slots", "count": 1, "max_total": 4,
  "conditions": [ { "condition": "minecraft:location_check",
                    "predicate": { "structures": "minecraft:ancient_city" } } ] }
```


