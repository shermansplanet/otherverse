import json

spiritTypes = ["earth", "air", "fire", "water", "phlogiston", "protection", "food", "flesh", "light", "technology", "dark", "cold", "overworld", "nether", "end", "death", "war", "nature", "fortune", "fate", "time", "white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow", "lime", "green", "cyan", "light_blue", "blue", "purple", "magenta", "pink"]
rootdir = "/Users/loren/Documents/modding/otherverse/src/main/resources/assets/otherverse/models/item/"
langdir = "/Users/loren/Documents/modding/otherverse/src/main/resources/assets/otherverse/lang/en_us.json";

with open(langdir) as infile:
    lang = json.load(infile)

for label in spiritTypes:
    fullname = "spirit_" + label;
    with open(rootdir + fullname + ".json", "w") as outfile:
        json.dump({"parent" : "item/generated", "textures" : {"layer0" : "otherverse:item/" + fullname}}, outfile)

    label = ' '.join([x[0].upper() + x[1:] for x in label.replace('_',' ').split(' ')])

    lang["item.otherverse." + fullname] = label + " Spirits"

with open(langdir, "w") as outfile:
    json.dump(lang, outfile, indent=4)