/**
 * Which texture of a current Minecraft version stands in for which texture of a legacy version -
 * the only thing of the "New Textures" pack we ship ourselves. The pictures are Mojang's and are
 * taken from the player's own copy of the game when the pack is built (see `newTexturesPack.ts`).
 *
 * All paths are below `assets/minecraft/textures/`, without `.png`.
 */
export interface NewTexturesRules {
  /** Counted up whenever the rules change, so packs built with older rules get rebuilt. */
  revision: number
  /**
   * Folders whose textures are taken from a new folder under the same file name unless
   * {@link renames} says otherwise - blocks and items mostly kept their names, the folders didn't.
   */
  folders: Record<string, string>
  /** Old texture -> new texture, for everything that was renamed or moved. */
  renames: Record<string, string>
  /**
   * Old textures (by prefix) that are laid out for a model - a mob, armor on a body. The new one is
   * only taken if it has exactly the old one's size; a model that was rebuilt since keeps its old
   * texture.
   */
  sameSizeOnly: string[]
  /** Old texture -> color (0xRRGGBB) the new, gray one is multiplied with - for what newer versions color at runtime and this one doesn't. */
  tinted: Record<string, number>
  /** Old texture that is one tall strip of frames -> the single frames it is put together from, top to bottom. */
  strips: Record<string, string[]>
  /** Old texture that is a grid of pictures -> its cells, row by row. */
  sheets: Record<string, { columns: number; cells: string[] }>
}

const COLORS = ['black', 'blue', 'brown', 'cyan', 'gray', 'green', 'light_blue', 'lime', 'magenta', 'orange', 'pink', 'purple', 'red', 'silver', 'white', 'yellow']
/** 1.8.9's name for a wood -> today's. */
const WOODS: Record<string, string> = { oak: 'oak', spruce: 'spruce', birch: 'birch', jungle: 'jungle', acacia: 'acacia', big_oak: 'dark_oak' }

/** "silver" became "light_gray" when the colors were named consistently. */
function newColor(color: string): string {
  return color === 'silver' ? 'light_gray' : color
}

function numbered(prefix: string, count: number): string[] {
  return Array.from({ length: count }, (_, index) => `${prefix}${String(index).padStart(2, '0')}`)
}

function blockRenames(): Record<string, string> {
  const blocks: Record<string, string> = {
    anvil_base: 'anvil',
    anvil_top_damaged_0: 'anvil_top',
    anvil_top_damaged_1: 'chipped_anvil_top',
    anvil_top_damaged_2: 'damaged_anvil_top',
    brick: 'bricks',
    cobblestone_mossy: 'mossy_cobblestone',
    command_block: 'command_block_side',
    comparator_off: 'comparator',
    repeater_off: 'repeater',
    redstone_torch_on: 'redstone_torch',
    torch_on: 'torch',
    redstone_lamp_off: 'redstone_lamp',
    furnace_front_off: 'furnace_front',
    deadbush: 'dead_bush',
    dirt_podzol_side: 'podzol_side',
    dirt_podzol_top: 'podzol_top',
    dispenser_front_horizontal: 'dispenser_front',
    dropper_front_horizontal: 'dropper_front',
    door_wood_lower: 'oak_door_bottom',
    door_wood_upper: 'oak_door_top',
    door_iron_lower: 'iron_door_bottom',
    door_iron_upper: 'iron_door_top',
    double_plant_fern_bottom: 'large_fern_bottom',
    double_plant_fern_top: 'large_fern_top',
    double_plant_grass_bottom: 'tall_grass_bottom',
    double_plant_grass_top: 'tall_grass_top',
    double_plant_paeonia_bottom: 'peony_bottom',
    double_plant_paeonia_top: 'peony_top',
    double_plant_rose_bottom: 'rose_bush_bottom',
    double_plant_rose_top: 'rose_bush_top',
    double_plant_syringa_bottom: 'lilac_bottom',
    double_plant_syringa_top: 'lilac_top',
    double_plant_sunflower_back: 'sunflower_back',
    double_plant_sunflower_bottom: 'sunflower_bottom',
    double_plant_sunflower_front: 'sunflower_front',
    double_plant_sunflower_top: 'sunflower_top',
    endframe_eye: 'end_portal_frame_eye',
    endframe_side: 'end_portal_frame_side',
    endframe_top: 'end_portal_frame_top',
    farmland_dry: 'farmland',
    farmland_wet: 'farmland_moist',
    fire_layer_0: 'fire_0',
    fire_layer_1: 'fire_1',
    flower_allium: 'allium',
    flower_blue_orchid: 'blue_orchid',
    flower_dandelion: 'dandelion',
    flower_houstonia: 'azure_bluet',
    flower_oxeye_daisy: 'oxeye_daisy',
    flower_rose: 'poppy',
    flower_tulip_orange: 'orange_tulip',
    flower_tulip_pink: 'pink_tulip',
    flower_tulip_red: 'red_tulip',
    flower_tulip_white: 'white_tulip',
    grass_side: 'grass_block_side',
    grass_side_overlay: 'grass_block_side_overlay',
    grass_side_snowed: 'grass_block_snow',
    grass_top: 'grass_block_top',
    hardened_clay: 'terracotta',
    ice_packed: 'packed_ice',
    itemframe_background: 'item_frame',
    melon_stem_connected: 'attached_melon_stem',
    melon_stem_disconnected: 'melon_stem',
    pumpkin_stem_connected: 'attached_pumpkin_stem',
    pumpkin_stem_disconnected: 'pumpkin_stem',
    mob_spawner: 'spawner',
    mushroom_block_skin_brown: 'brown_mushroom_block',
    mushroom_block_skin_red: 'red_mushroom_block',
    mushroom_block_skin_stem: 'mushroom_stem',
    mushroom_brown: 'brown_mushroom',
    mushroom_red: 'red_mushroom',
    nether_brick: 'nether_bricks',
    noteblock: 'note_block',
    piston_top_normal: 'piston_top',
    portal: 'nether_portal',
    prismarine_dark: 'dark_prismarine',
    prismarine_rough: 'prismarine',
    pumpkin_face_off: 'carved_pumpkin',
    pumpkin_face_on: 'jack_o_lantern',
    quartz_block_chiseled: 'chiseled_quartz_block',
    quartz_block_chiseled_top: 'chiseled_quartz_block_top',
    quartz_block_lines: 'quartz_pillar_side',
    quartz_block_lines_top: 'quartz_pillar_top',
    quartz_ore: 'nether_quartz_ore',
    rail_activator: 'activator_rail',
    rail_activator_powered: 'activator_rail_on',
    rail_detector: 'detector_rail',
    rail_detector_powered: 'detector_rail_on',
    rail_golden: 'powered_rail',
    rail_golden_powered: 'powered_rail_on',
    rail_normal: 'rail',
    rail_normal_turned: 'rail_corner',
    red_sandstone_carved: 'chiseled_red_sandstone',
    red_sandstone_normal: 'red_sandstone',
    red_sandstone_smooth: 'cut_red_sandstone',
    sandstone_carved: 'chiseled_sandstone',
    sandstone_normal: 'sandstone',
    sandstone_smooth: 'cut_sandstone',
    reeds: 'sugar_cane',
    slime: 'slime_block',
    sponge_wet: 'wet_sponge',
    stone_andesite: 'andesite',
    stone_andesite_smooth: 'polished_andesite',
    stone_diorite: 'diorite',
    stone_diorite_smooth: 'polished_diorite',
    stone_granite: 'granite',
    stone_granite_smooth: 'polished_granite',
    stone_slab_side: 'smooth_stone_slab_side',
    stone_slab_top: 'smooth_stone',
    stonebrick: 'stone_bricks',
    stonebrick_carved: 'chiseled_stone_bricks',
    stonebrick_cracked: 'cracked_stone_bricks',
    stonebrick_mossy: 'mossy_stone_bricks',
    tallgrass: 'short_grass',
    trapdoor: 'oak_trapdoor',
    trip_wire: 'tripwire',
    trip_wire_source: 'tripwire_hook',
    waterlily: 'lily_pad',
    web: 'cobweb'
  }
  const crops: [string, number][] = [
    ['carrots', 4],
    ['potatoes', 4],
    ['wheat', 8],
    ['nether_wart', 3],
    ['cocoa', 3]
  ]
  for (const [crop, stages] of crops) {
    for (let stage = 0; stage < stages; stage++) {
      blocks[`${crop}_stage_${stage}`] = `${crop}_stage${stage}`
    }
  }
  for (const [oldWood, wood] of Object.entries(WOODS)) {
    blocks[`leaves_${oldWood}`] = `${wood}_leaves`
    blocks[`log_${oldWood}`] = `${wood}_log`
    blocks[`log_${oldWood}_top`] = `${wood}_log_top`
    blocks[`planks_${oldWood}`] = `${wood}_planks`
    // The one place 1.8.9 calls dark oak "roofed oak"; its doors already say "dark_oak".
    blocks[`sapling_${oldWood === 'big_oak' ? 'roofed_oak' : oldWood}`] = `${wood}_sapling`
    if (oldWood !== 'oak') {
      blocks[`door_${wood}_lower`] = `${wood}_door_bottom`
      blocks[`door_${wood}_upper`] = `${wood}_door_top`
    }
  }
  for (const color of COLORS) {
    blocks[`glass_${color}`] = `${newColor(color)}_stained_glass`
    blocks[`glass_pane_top_${color}`] = `${newColor(color)}_stained_glass_pane_top`
    blocks[`hardened_clay_stained_${color}`] = `${newColor(color)}_terracotta`
    blocks[`wool_colored_${color}`] = `${newColor(color)}_wool`
  }
  return blocks
}

function itemRenames(): Record<string, string> {
  const items: Record<string, string> = {
    apple_golden: 'golden_apple',
    beef_cooked: 'cooked_beef',
    beef_raw: 'beef',
    boat: 'oak_boat',
    book_enchanted: 'enchanted_book',
    book_normal: 'book',
    book_writable: 'writable_book',
    book_written: 'written_book',
    bow_standby: 'bow',
    bucket_empty: 'bucket',
    bucket_lava: 'lava_bucket',
    bucket_milk: 'milk_bucket',
    bucket_water: 'water_bucket',
    carrot_golden: 'golden_carrot',
    chicken_cooked: 'cooked_chicken',
    chicken_raw: 'chicken',
    door_wood: 'oak_door',
    door_iron: 'iron_door',
    door_acacia: 'acacia_door',
    door_birch: 'birch_door',
    door_dark_oak: 'dark_oak_door',
    door_jungle: 'jungle_door',
    door_spruce: 'spruce_door',
    fireball: 'fire_charge',
    fireworks: 'firework_rocket',
    fireworks_charge: 'firework_star',
    fireworks_charge_overlay: 'firework_star_overlay',
    fish_clownfish_raw: 'tropical_fish',
    fish_cod_cooked: 'cooked_cod',
    fish_cod_raw: 'cod',
    fish_pufferfish_raw: 'pufferfish',
    fish_salmon_cooked: 'cooked_salmon',
    fish_salmon_raw: 'salmon',
    fishing_rod_uncast: 'fishing_rod',
    gold_horse_armor: 'golden_horse_armor',
    melon: 'melon_slice',
    melon_speckled: 'glistering_melon_slice',
    minecart_chest: 'chest_minecart',
    minecart_command_block: 'command_block_minecart',
    minecart_furnace: 'furnace_minecart',
    minecart_hopper: 'hopper_minecart',
    minecart_normal: 'minecart',
    minecart_tnt: 'tnt_minecart',
    mutton_cooked: 'cooked_mutton',
    mutton_raw: 'mutton',
    netherbrick: 'nether_brick',
    porkchop_cooked: 'cooked_porkchop',
    porkchop_raw: 'porkchop',
    potato_baked: 'baked_potato',
    potato_poisonous: 'poisonous_potato',
    potion_bottle_drinkable: 'potion',
    potion_bottle_empty: 'glass_bottle',
    potion_bottle_splash: 'splash_potion',
    rabbit_cooked: 'cooked_rabbit',
    rabbit_raw: 'rabbit',
    redstone_dust: 'redstone',
    reeds: 'sugar_cane',
    seeds_melon: 'melon_seeds',
    seeds_pumpkin: 'pumpkin_seeds',
    seeds_wheat: 'wheat_seeds',
    sign: 'oak_sign',
    slimeball: 'slime_ball',
    spider_eye_fermented: 'fermented_spider_eye',
    wooden_armorstand: 'armor_stand',
    // Not here on purpose: map_empty and map_filled keep their old textures (own user request - the
    // current ones don't look good in this version).
    // Four of 1.8.9's dyes are items of their own today.
    dye_powder_black: 'ink_sac',
    dye_powder_blue: 'lapis_lazuli',
    dye_powder_brown: 'cocoa_beans',
    dye_powder_white: 'bone_meal'
  }
  for (const color of COLORS) {
    items[`dye_powder_${color}`] ??= `${newColor(color)}_dye`
  }
  for (const part of ['axe', 'hoe', 'pickaxe', 'shovel', 'sword']) {
    items[`gold_${part}`] = `golden_${part}`
    items[`wood_${part}`] = `wooden_${part}`
  }
  for (const part of ['boots', 'chestplate', 'helmet', 'leggings']) {
    items[`gold_${part}`] = `golden_${part}`
  }
  for (const record of ['11', '13', 'blocks', 'cat', 'chirp', 'far', 'mall', 'mellohi', 'stal', 'strad', 'wait', 'ward']) {
    items[`record_${record}`] = `music_disc_${record}`
  }
  return items
}

/**
 * Mobs and objects whose texture is still the old size and, compared pixel by pixel, still filled
 * in the same places. Everything not listed keeps its old texture - above all what was rebuilt
 * since: horses, rabbits, cows, pigs, villagers, chests, boats, the wither, the ghast, the bat.
 */
function entityRenames(): Record<string, string> {
  const entities: Record<string, string> = {
    'entity/alex': 'entity/player/slim/alex',
    'entity/steve': 'entity/player/wide/steve',
    'entity/armorstand/wood': 'entity/armorstand/armorstand',
    'entity/arrow': 'entity/projectiles/arrow',
    'entity/beacon_beam': 'entity/beacon/beacon_beam',
    'entity/blaze': 'entity/blaze/blaze',
    // 1.8.9's "black" cat is the black and white one.
    'entity/cat/black': 'entity/cat/cat_black',
    'entity/cat/red': 'entity/cat/cat_red',
    'entity/cat/siamese': 'entity/cat/cat_siamese',
    'entity/chicken': 'entity/chicken/chicken_temperate',
    'entity/enchanting_table_book': 'entity/enchantment/enchanting_table_book',
    'entity/end_portal': 'entity/end_portal/end_portal',
    'entity/endercrystal/endercrystal': 'entity/end_crystal/end_crystal',
    'entity/endercrystal/endercrystal_beam': 'entity/end_crystal/end_crystal_beam',
    'entity/endermite': 'entity/endermite/endermite',
    'entity/experience_orb': 'entity/experience/experience_orb',
    'entity/guardian_beam': 'entity/guardian/guardian_beam',
    'entity/guardian_elder': 'entity/guardian/guardian_elder',
    'entity/iron_golem': 'entity/iron_golem/iron_golem',
    'entity/lead_knot': 'entity/lead_knot/lead_knot',
    'entity/sheep/sheep_fur': 'entity/sheep/sheep_wool',
    'entity/silverfish': 'entity/silverfish/silverfish',
    'entity/snowman': 'entity/snow_golem/snow_golem',
    'entity/spider_eyes': 'entity/spider/spider_eyes',
    'entity/squid': 'entity/squid/squid',
    'entity/witch': 'entity/witch/witch'
  }
  // Still where they were.
  const unmoved = [
    'cat/ocelot',
    'creeper/creeper',
    'creeper/creeper_armor',
    'enderdragon/dragon',
    'enderdragon/dragon_exploding',
    'enderdragon/dragon_eyes',
    'enderman/enderman',
    'enderman/enderman_eyes',
    'sheep/sheep',
    'skeleton/skeleton',
    'skeleton/wither_skeleton',
    'slime/slime',
    'spider/cave_spider',
    'spider/spider',
    'wither/wither_armor',
    'wolf/wolf',
    'wolf/wolf_angry',
    'wolf/wolf_collar',
    'wolf/wolf_tame',
    'zombie/zombie'
  ]
  for (const name of unmoved) {
    entities[`entity/${name}`] = `entity/${name}`
  }
  // Armor as worn: layer 1 is everything but the leggings.
  for (const material of ['chainmail', 'diamond', 'gold', 'iron', 'leather']) {
    entities[`models/armor/${material}_layer_1`] = `entity/equipment/humanoid/${material}`
    entities[`models/armor/${material}_layer_2`] = `entity/equipment/humanoid_leggings/${material}`
  }
  entities['models/armor/leather_layer_1_overlay'] = 'entity/equipment/humanoid/leather_overlay'
  entities['models/armor/leather_layer_2_overlay'] = 'entity/equipment/humanoid_leggings/leather_overlay'
  return entities
}

function prefixed(oldFolder: string, newFolder: string, renames: Record<string, string>): Record<string, string> {
  return Object.fromEntries(Object.entries(renames).map(([from, to]) => [`${oldFolder}/${from}`, `${newFolder}/${to}`]))
}

/** What 1.8.9 draws water with before biome colors existed for it - the plains color newer versions tint the gray texture with. */
const WATER_COLOR = 0x3f76e4

const RULES_1_8_9: NewTexturesRules = {
  revision: 2,
  folders: { blocks: 'block', items: 'item', 'entity/banner': 'entity/banner' },
  renames: {
    ...prefixed('blocks', 'block', blockRenames()),
    ...prefixed('items', 'item', itemRenames()),
    ...entityRenames(),
    // The empty armor slots of the inventory were item textures.
    'items/empty_armor_slot_helmet': 'gui/sprites/container/slot/helmet',
    'items/empty_armor_slot_chestplate': 'gui/sprites/container/slot/chestplate',
    'items/empty_armor_slot_leggings': 'gui/sprites/container/slot/leggings',
    'items/empty_armor_slot_boots': 'gui/sprites/container/slot/boots'
  },
  sameSizeOnly: ['entity/', 'models/'],
  tinted: { 'blocks/water_still': WATER_COLOR, 'blocks/water_flow': WATER_COLOR },
  strips: { 'items/clock': numbered('item/clock_', 64), 'items/compass': numbered('item/compass_', 32) },
  sheets: {
    'environment/moon_phases': {
      columns: 4,
      cells: ['full_moon', 'waning_gibbous', 'third_quarter', 'waning_crescent', 'new_moon', 'waxing_crescent', 'first_quarter', 'waxing_gibbous'].map(
        (phase) => `environment/celestial/moon/${phase}`
      )
    }
  }
}

/** Per legacy version; a version without rules has no "New Textures" pack. */
export const NEW_TEXTURES_RULES: Record<string, NewTexturesRules> = { '1.8.9': RULES_1_8_9 }
