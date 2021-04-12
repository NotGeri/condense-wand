# CondenseWand
Made for a request on [bloom.host](https://bloom.host)  - Feel free to copy / modify / improve!

### Commands & Permissions
- `/condensewand` (`condensewand.give`) - Give a wand to yourself.
- `/condensewand reload` (`condensewand.reload`) - Reload the plugin's configuration file.
- `/condensewand give <player name>` (`condensewand.give.other`) - Give a wand to another online player.

### Example configuration file: 
```yml
messages:
  player: "&cUnable to find player: %player%" # Available placeholders: %player%
  reload: "&aSuccessfully reloaded the plugin!"
  console: "&cOnly players may execute this command!"
  success: "&aSuccessfully given yourself a wand."
  permission: "&cYou do not have access to this command!"
  success-other: "&aSuccessfully given %player% a wand!" # Available placeholders: %player%

settings:
  particle:
    # Be sure to use particles from https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Particle.html
    enabled: true
    name: FLAME
    amount: 50

  sound:
    # Be sure to use sounds from https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html
    # The pitch has to be between 0.0 and 2.0.
    enabled: true
    name: ENTITY_GENERIC_EXPLODE
    pitch: 0
    volume: 10

  convert:
    IRON_INGOT: # The item the transform
      required: 9 # The amount of item to transform
      to: IRON_BLOCK # The result item of the transformation
      amount: 1 # The result amount of the transformation

    GOLD_INGOT:
      required: 9
      to: GOLD_BLOCK
      amount: 1

  delete:
    - STONE_SWORD
    - STONE_AXE

item:
  material: DIAMOND_AXE # Be sure to use a material from https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html
  glowing: true # Whether or not the item has the enchant glint
  name: "&a&lEpic Wand yes"
  lore:
    - "&f&m--------------------------"
    - "&7Very epic indeed,"
    - "&7just right click chests!"
    - "&f&m--------------------------"
```

