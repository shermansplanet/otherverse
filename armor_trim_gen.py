armor_types = ["helmet","chestplate","leggings","boots"]
trim_types = ["quartz", "iron", "netherite", "redstone", "copper", "gold", "emerald", "diamond", "lapis", "amethyst"]
template = '''{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "otherverse:item/ruins_ARMOR",
    "layer1": "minecraft:trims/items/ARMOR_trim_TRIM"
  }
}'''
for armor_type in armor_types:
    for trim_type in trim_types:
        with open(f"src/main/resources/assets/otherverse/models/item/ruins_{armor_type}_{trim_type}_trim.json", "w") as file:
            file.write(template.replace("ARMOR", armor_type).replace("TRIM",trim_type))
