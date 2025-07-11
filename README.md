# Tiered More

![icon.png](src%2Fmain%2Fresources%2Fassets%2Ftiered%2Ficon.png)

### Usage:
- Head [here](https://github.com/Globox1997/tiered/blob/1.21/README.md) since most of everything is the same.

### New features

- Optional field "cursed": "true" / "false"
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
