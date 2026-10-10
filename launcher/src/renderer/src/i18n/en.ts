import { de } from './de'

/**
 * English strings, typed against `typeof de` (not its own independent interface) - TypeScript
 * then flags a missing/mistyped key here as a compile error the moment `de.ts` gains one, instead
 * of it silently falling back to German (or nothing) at runtime.
 */
export const en: typeof de = {
  common: {
    back: 'Back',
    cancel: 'Cancel',
    save: 'Save',
    delete: 'Delete',
    remove: 'Remove',
    loading: 'Loading…',
    hide: 'Hide'
  },
  app: {
    checkingSession: 'Checking session…'
  },
  login: {
    title: "TNT's All-In-1 Client",
    subtitle: 'Sign in with your Microsoft account to play.',
    loginButton: 'Sign in with Microsoft',
    playOffline: (p: { name: string }) => `Play offline as ${p.name}`
  },
  onboarding: {
    colorsHeading: 'Customize the colors',
    colorsExplanation:
      "You can customize the launcher's colors entirely to your taste - this affects the whole client. You can also skip this step and keep the default colors.",
    skip: 'Skip (default colors)',
    next: 'Next',
    storageHeading: 'Choose where to store your game data',
    storageExplanation:
      'This is where the launcher keeps Minecraft, your instances and your worlds - over time that can add up to many gigabytes. No matter where you installed the launcher, it starts out in your user folder (see below). If space is tight there, it is best to pick a separate, empty folder on another drive. You can also change this later in the settings.',
    storageMoving: 'Moving…',
    designHeading: 'Choose the in-game design',
    designExplanation:
      'The title screen and the Client Mods menu can look like regular Minecraft or use the client design in your colors. You can change this anytime in the settings or right on the in-game title screen.'
  },
  tour: {
    questionTitle: 'Guided tour',
    questionText: 'Would you like a guided tour through the client to get to know it and all of its areas? It only takes a few minutes.',
    questionLater: 'You can also start the tour anytime later in the settings.',
    yes: 'Yes, start the tour',
    no: 'No, thanks',
    counter: (current: number, total: number) => `Step ${current} of ${total}`,
    back: 'Back',
    next: 'Next',
    finish: 'Done',
    end: 'End tour',
    waitingForInstance: 'Create an instance to continue.',
    instanceCreated: 'Great, your instance has been created!',
    skipWaiting: 'Continue without creating',
    inGameQuestion:
      'Would you like to see the menus in the game too? Then the selected instance starts now and the tour continues in the game.',
    inGameYes: 'Yes, start the game',
    inGameNo: 'No, done',
    steps: {
      account: {
        title: 'Your account',
        text: 'Top left you can see which Minecraft account you are signed in with - your name and your skin’s head. Clicking it opens a small menu where you can sign out. That’s locked during the tour.'
      },
      settingsButton: {
        title: 'Settings',
        text: '“Settings” is where you adjust the launcher to your liking. Let’s take a quick look inside.'
      },
      settingsMemory: {
        title: 'Memory and storage location',
        text: 'Here you set how much memory (RAM) Minecraft may use - “Automatic” suits most people. Below it you can see where Minecraft, your instances and your worlds are stored, and move all of it to another drive.'
      },
      settingsGeneral: {
        title: 'Snapshots and console',
        text: '“Show snapshots” adds Minecraft’s test versions to the version picker. The console shows what’s happening while the game starts - in its own window if you like, instead of here in the launcher.'
      },
      settingsAppearance: {
        title: 'Appearance',
        text: 'Here you change the launcher’s colors - the same ones you could pick at the start. The preview shows each color before you apply it.'
      },
      settingsLanguage: {
        title: 'Language, design and this tour',
        text: 'Here you switch the language and choose whether the in-game title screen and Client Mods menu look like regular Minecraft or use the client design in your colors. And this is where you can restart this tour anytime.'
      },
      credits: {
        title: 'Credits',
        text: '“Credits” lists every mod, resource pack and font the client ships with - with its license and a link to the people who made it.'
      },
      skinButton: {
        title: 'Skin',
        text: '“Skin” is where you manage how your character looks.'
      },
      skinCurrent: {
        title: 'Your current skin',
        text: 'This is how your character looks right now. Drag with the mouse to rotate it, use the mouse wheel to zoom.'
      },
      skinLibrary: {
        title: 'My skins',
        text: 'This is your skin collection. “Create new skin” opens the skin editor, where you paint right on the 3D model. “Use” turns a skin into your actual Minecraft skin.'
      },
      skinUpload: {
        title: 'Upload a skin from your PC',
        text: 'Already have a skin file (PNG)? Upload it right here - it gets added to your collection too.'
      },
      capesButton: {
        title: 'Capes',
        text: '“Capes” is where you design your own cape. Every other player who uses this client can see it.'
      },
      capesCreate: {
        title: 'Create a cape',
        text: 'Add a finished cape image, turn any picture into a cape with “Convert image to cape”, or paint one with “Draw cape”. The preview shows it on your character right away.'
      },
      capesCollection: {
        title: 'My capes',
        text: 'All your capes end up here. “Activate” wears a cape in the game - you can take it off or edit it anytime.'
      },
      capesRules: {
        title: 'Rules for capes',
        text: 'Other players see your cape too. This is what is not allowed - whoever ignores it loses their cape and may no longer upload custom capes.'
      },
      capesReport: {
        title: 'Report a cape',
        text: "Is someone wearing a cape that breaks the rules? Enter their Minecraft name here. The client's team looks at the cape; the player is not told who reported them."
      },
      friendsButton: {
        title: 'Friends',
        text: '“Friends” shows which of your friends are online right now. A number next to it means open friend requests.'
      },
      friendsOwn: {
        title: 'Your status',
        text: 'Choose how others see you: online, away, do not disturb or invisible. You can also hide which server you’re playing on.'
      },
      friendsAdd: {
        title: 'Add a friend',
        text: 'Enter a friend’s Minecraft name and send them a request. This works for everyone who has used the client at least once.'
      },
      friendsList: {
        title: 'Your friends',
        text: 'Here you see your friends and what they’re playing. If someone is on a server, “Join” takes you straight there. When a friend invites you into their world, a notice shows up at the top.'
      },
      instancesButton: {
        title: 'Instances',
        text: 'An instance is a Minecraft installation of its own, with its own version, mods and worlds. That way you can have the same version once with and once without certain mods, for example.'
      },
      instancesCreate: {
        title: 'Create an instance',
        text: 'Try it right away: enter a name, pick a Minecraft version and click “Create”.'
      },
      instancesList: {
        title: 'Your instances',
        text: 'All your instances are listed here. You can select, rename, duplicate or delete them. “Import from another client…” above brings over mods and settings from another launcher.'
      },
      instancePicker: {
        title: 'Pick an instance',
        text: 'This is where you choose which instance starts when you click “Play”. Mods, worlds and resource packs always belong to the instance picked here.'
      },
      modsButton: {
        title: 'Mods',
        text: '“Mods” is where you manage the mods of the selected instance.'
      },
      modsBundled: {
        title: 'Bundled mods',
        text: 'The client brings these mods along itself, for example Sodium and Lithium for more FPS. Most of them always run - the ones that can be turned off are switched here.'
      },
      modsSearch: {
        title: 'Browse mods',
        text: 'Browse thousands of mods from Modrinth here. “Install” automatically downloads the right version for your instance.'
      },
      modsLegacy: {
        title: 'Mods in this version',
        text: 'Minecraft versions before 1.14 launch without external mods - so nothing can be added here. Where the client mods exist for the version (for example 1.8.9), they are in the game under “Client Mods”.'
      },
      modsOwn: {
        title: 'Your mods',
        text: 'Already have a mod file (.jar) on your PC? Add it here. Every mod can be switched off and on or removed on its own.'
      },
      worldsButton: {
        title: 'Worlds',
        text: '“Worlds” shows the singleplayer worlds of the selected instance.'
      },
      worlds: {
        title: 'Manage worlds',
        text: 'Upload worlds from your PC - as a folder or ZIP, for example from the regular Minecraft launcher. You can copy or move worlds to another instance and remove them (they go to the recycle bin).'
      },
      resourcepacksButton: {
        title: 'Resource packs',
        text: '“Resource Packs” is where you manage the resource packs of the selected instance.'
      },
      resourcepacks: {
        title: 'Manage resource packs',
        text: 'Upload your own resource packs or remove them. You turn them on in the game under Options → Resource Packs. The packs the client brings along are managed by the client itself and aren’t listed here.'
      },
      play: {
        title: 'Let’s go!',
        text: '“Play” starts the selected instance. On the first start the launcher downloads Minecraft, which can take a few minutes. Have fun!'
      }
    }
  },
  play: {
    headerSkin: 'Skin',
    headerCapes: 'Capes',
    headerCredits: 'Credits',
    headerSettings: 'Settings',
    headerLogout: 'Sign out',
    playerMenuLabel: 'Account menu',
    update: {
      available: (version: string) => `Update found (version ${version}) - downloading…`,
      downloading: (percent: number) => `Downloading update… (${percent}%)`,
      downloaded: (version: string) => `Update downloaded (version ${version}) - ready to install.`,
      restartNow: 'Restart now'
    },
    offline: {
      banner:
        'Offline mode: no internet connection. You can play singleplayer and LAN; servers, Discord presence, skin/cape upload, mod search and updates need internet.',
      reconnect: 'Reconnect',
      reconnecting: 'Connecting…',
      stillOffline: 'Still no connection.'
    },
    modBundleUpdate: {
      available: (names: string) => `A new mod bundle version is available (${names}).`,
      ownMod: 'own mod',
      applying: 'Updating…',
      apply: 'Update'
    },
    instanceLabel: 'Instance',
    noInstance: 'No instance',
    manageInstances: 'Manage instances…',
    mods: 'Mods…',
    worlds: 'Worlds…',
    resourcepacks: 'Resource Packs…',
    versionListError: (error: string) => `Couldn't load the version list: ${error}`,
    noInstanceWarning: 'No instance created yet - create one via "Manage instances…".',
    bundleIncompatibleWarning: (versionId: string) =>
      `${versionId} has no bundled mods/resource packs (Sodium, Lithium, our own client mod, …) — launches as plain Fabric+vanilla without mods.`,
    legacyWarning: (versionId: string) =>
      `${versionId} launches as plain Minecraft — without mods and without the client features.`,
    legacyClientNote: (versionId: string) => `${versionId} launches without external mods — the client mods are available though.`,
    play: 'Play',
    playing: 'Running…',
    cancel: 'Cancel Launch'
  },
  instances: {
    title: 'Instances',
    newInstanceHeading: 'New instance',
    nameLabel: 'Name:',
    defaultName: (n: number) => `Instance ${n}`,
    create: 'Create',
    importing: 'Importing…',
    importFromClient: 'Import from another client…',
    importedSettingsWarning:
      'Imported settings (options.txt) apply to all your instances, not just the new one - this launcher deliberately shares these settings across all instances.',
    importResultMods: (count: number) => `${count} mod(s) imported`,
    importResultSkipped: (count: number) => `${count} skipped (already included in the client or not compatible)`,
    importResultResourcepacks: (count: number) => `${count} resource pack(s) imported`,
    importResultWorlds: (count: number) => `${count} world(s) imported`,
    importResultOptions: 'Settings imported',
    importResultNone: 'No settings, mods, resource packs or worlds found in the selected folder.',
    versionListError: (error: string) => `Couldn't load the version list: ${error}`,
    existingHeading: 'Existing instances',
    active: 'Active',
    clientSupported: 'This version is supported by the client',
    supersededVersionHint: (versionId: string, fixedVersion: string) =>
      `${versionId} is a faulty version that Mojang replaced with ${fixedVersion} shortly afterwards. We recommend using version ${fixedVersion}, as it runs more stably and is therefore the one the client supports.`,
    select: 'Select',
    rename: 'Rename',
    cloning: 'Duplicating…',
    clone: 'Duplicate',
    copySuffix: (name: string) => `${name} (copy)`,
    deleteConfirm: (name: string) =>
      `Really delete "${name}"? Saves, settings and mods of this instance will be lost permanently.`,
    empty: 'No instance created yet.'
  },
  mods: {
    title: 'Mods',
    bundledHeading: 'Bundled mods',
    bundledInfo:
      'Off by default - enable individually here. Fabric API and Sodium/Lithium (performance, for good framerates even on weaker hardware) as well as Continuity (has its own in-game on/off in the mod menu) always run and therefore don\'t show up as their own toggles.',
    bundleIncompatible: (versionId: string) => `Has no effect right now - ${versionId} has no mod bundle, so it launches without bundled mods anyway.`,
    legacyNoMods: (versionId: string) =>
      `${versionId} launches as plain Minecraft without a mod loader - mods can't be used here for versions before 1.14.`,
    legacyClientModsOnly: (versionId: string) =>
      `${versionId} launches without external mods - none can be added here for versions before 1.14. The client mods are available though: you'll find them in the game under “Client Mods”.`,
    bundledSource:
      'The launcher downloads the bundled mods straight from Modrinth - the first launch of a version needs an internet connection for that. Please report problems with these mods (e.g. Sodium) to us, not to their developers: they don\'t give support for clients like this one.',
    bundledLoading: 'Downloading the bundled mods from Modrinth…',
    noToggleable: 'Nothing to toggle right now - every currently bundled mod always runs (see the note above).',
    searchHeading: 'Browse & discover mods',
    searchUnavailable: 'Modrinth search is only available for mod-bundle-compatible versions.',
    searchPlaceholder: 'Search mods (leave empty to browse)…',
    searching: 'Searching…',
    search: 'Search',
    alreadyBundled: 'Already bundled',
    installed: 'Installed',
    installing: 'Installing…',
    install: 'Install',
    noResults: 'No mods found.',
    ownHeading: 'Your mods',
    addMod: 'Add mod…',
    noneAdded: 'No mods of your own added yet.',
    notices: {
      essential:
        "Essential detected: while it's installed, the game runs in the Minecraft design (Essential's menus can't be restyled), and Essential's keys are set to \"not bound\" on the first start. Essential cosmetics can cover client capes.",
      optifine: "OptiFine/OptiFabric isn't compatible with Sodium - the game won't start with it. Use Iris for shaders instead."
    },
    sortOptions: {
      relevance: 'Relevance',
      downloads: 'Downloads',
      follows: 'Followers',
      newest: 'Newest',
      updated: 'Recently updated'
    },
    sortAriaLabel: 'Sort order',
    pagesAriaLabel: 'Pages'
  },
  worlds: {
    title: 'Worlds',
    heading: "This instance's worlds",
    empty: 'No worlds in this instance.',
    copy: 'Copy',
    move: 'Move',
    needsAnotherInstance: 'Copying/moving needs at least one other instance - create a second one first.',
    movedStatus: (world: string, target: string) => `Moved world "${world}" to "${target}".`,
    copiedStatus: (world: string, target: string, copiedTo: string) =>
      `Copied world "${world}" to "${target}" (as "${copiedTo}" there).`,
    actionTitleMove: (world: string) => `Move world "${world}"`,
    actionTitleCopy: (world: string) => `Copy world "${world}"`,
    targetInstanceLabel: 'Target instance:',
    moveWarning:
      'Warning: different Minecraft versions or mods between instances can corrupt this world or cause crashes (e.g. missing mod blocks/items not installed in the target instance, or a chunk format an older version can\'t load). This is at your own risk and cannot be undone afterwards.',
    confirm: 'Confirm',
    working: 'Working…',
    uploadInfo:
      'Bring worlds from your PC into this instance - as a folder or as a ZIP. Picking a whole saves folder brings in every world inside it. The original stays untouched.',
    uploadFolder: 'Upload world folder…',
    uploadZip: 'Upload world ZIP…',
    uploadDialogFolder: 'Select world folder(s)',
    uploadDialogZip: 'Select world ZIP(s)',
    uploading: 'Uploading…',
    actionTitleDelete: (world: string) => `Remove world "${world}"`,
    deleteInfo: 'The world is moved to the recycle bin - you can restore it from there if needed.',
    deletedStatus: (world: string) => `World "${world}" moved to the recycle bin.`,
    uploadedStatus: (worlds: string[]) =>
      worlds.length === 1 ? `World "${worlds[0]}" uploaded.` : `${worlds.length} worlds uploaded: ${worlds.join(', ')}.`
  },
  friends: {
    title: 'Friends',
    headerButton: (incoming: number) => (incoming > 0 ? `Friends (${incoming})` : 'Friends'),
    enabledLabel: 'Friends and online status',
    enabledInfo:
      'While this is on, the launcher reports your status and what you are playing to the client\'s server every 20 seconds and asks it which of the players around you use the client (for the logo next to their name). Switched off, the launcher never contacts that server on its own. What exactly is stored is listed in PRIVACY.md on the project\'s GitHub page.',
    disabledInfo:
      'Friends are switched off: no online status, no friends list, no invitations and no client logos in the game. Your friends stay saved and are back as soon as you switch it on.',
    ownHeading: 'Your status',
    statusLabel: 'Status:',
    ownStatus: { online: 'Online', away: 'Away', dnd: 'Do not disturb', invisible: 'Invisible' },
    visibleStatus: { online: 'Online', away: 'Away', dnd: 'Do not disturb', offline: 'Offline' },
    hideServer: 'Hide server from friends',
    addHeading: 'Add friend',
    addInfo: 'Enter their Minecraft name. You can only add players who have used the client before.',
    addPlaceholder: 'Minecraft name',
    addButton: 'Send request',
    requestSent: (name: string) => `Request sent to ${name}.`,
    nowFriends: (name: string) => `You and ${name} are now friends.`,
    requestsHeading: 'Requests',
    incoming: 'Wants to be your friend',
    outgoing: 'Request sent, waiting for an answer',
    accept: 'Accept',
    decline: 'Decline',
    withdraw: 'Withdraw',
    friendsHeading: (count: number) => `Friends (${count})`,
    empty: 'No friends yet - send a request above.',
    confirmRemove: (name: string) => `Remove ${name} from your friends list?`,
    block: 'Block',
    unblock: 'Unblock',
    confirmBlock: (name: string) =>
      `Block ${name}? A friendship and open requests between you are removed, and ${name} can no longer send you requests. ${name} is not told about it.`,
    blockedHeading: (count: number) => `Blocked (${count})`,
    deleteData: 'Delete my data from the server…',
    confirmDeleteData:
      "Delete everything the client's server has stored about you? That is your friends, open requests, blocked players, your status and your active cape. This cannot be undone. \"Friends and online status\" is switched off along with it - if you switch it back on, you start again with an empty list. Your cape collection on this PC stays.",
    deleteDataConfirm: 'Delete everything',
    dataDeleted: 'Your data on the server has been deleted.',
    join: 'Join',
    joinInGameHint: 'The game is already running - you get connected there directly.',
    inviteText: (name: string, version: string) => `${name} invites you into their world (Minecraft ${version}).`,
    noInstanceForVersion: (version: string) => `You need an instance with Minecraft ${version} - create one under "Instances".`,
    noInstance: 'Select an instance first.',
    activity: {
      launcher: 'In the launcher',
      menu: 'In the main menu',
      singleplayer: 'Playing singleplayer',
      multiplayer: 'Playing multiplayer',
      playing: 'Playing Minecraft',
      server: (server: string) => `Playing on ${server}`
    }
  },
  capeRules: {
    heading: 'Rules for capes',
    intro: 'Your active cape is public: every player using this client sees it on you. Not allowed are capes with:',
    items: [
      'nudity, pornography or other sexual content',
      'hate, insults or discrimination, for example because of origin, religion, gender or sexuality',
      'banned symbols or glorification of violence',
      'anything that breaks the law'
    ],
    consequence:
      "The client's team may remove a cape at its own discretion and ban the account from further uploads - also when a cape matches none of these points but does not fit the client."
  },
  capeReport: {
    heading: 'Report a cape',
    info: "Is someone wearing a custom cape that is offensive or illegal? Enter their Minecraft name. The cape is saved as it is right now and looked at by the client's team. The reported player is not told who reported them.",
    namePlaceholder: 'Minecraft name',
    reasonPlaceholder: 'What is the problem? (optional)',
    send: 'Report',
    sent: (name: string) => `The cape of ${name} has been reported. Thank you!`
  },
  moderation: {
    heading: (count: number) => `Reported capes (${count})`,
    info: 'Only visible to you as a moderator. The cape is shown as it was reported.',
    refresh: 'Reload',
    empty: 'No open reports.',
    reportCount: (count: number) => (count === 1 ? '1 report' : `${count} reports`),
    stillWorn: 'still worn',
    replaced: 'replaced or removed since',
    dismiss: 'Dismiss',
    remove: 'Remove cape',
    ban: 'Ban',
    unban: 'Lift ban',
    confirmDismiss: (name: string) => `Dismiss the reports about ${name}? The cape stays.`,
    confirmRemove: (name: string) => `Remove the cape of ${name}? ${name} can upload a new one afterwards.`,
    confirmBan: (name: string) => `Remove the cape of ${name} and ban the account from uploading? The reason is stored.`,
    banReasonPlaceholder: 'Reason for the ban',
    bansHeading: (count: number) => `Banned accounts (${count})`
  },
  resourcepacks: {
    title: 'Resource Packs',
    heading: (instance: string) => `External resource packs in "${instance}"`,
    info: "Only your own packs are listed here - the client's bundled ones are managed automatically. Enable a pack in-game under Options → Resource Packs.",
    add: 'Upload pack…',
    dialogTitle: 'Choose resource pack(s)',
    removeAll: 'Remove all',
    empty: 'No resource packs of your own in this instance yet.',
    confirmRemove: (name: string) => `Really delete resource pack "${name}" from this instance?`,
    confirmRemoveAll: (count: number) =>
      `Really delete all ${count} of your resource packs from this instance? The bundled packs stay.`
  },
  skin: {
    renameTitle: 'Name your skin',
    title: 'Skins',
    currentHeading: 'Current skin',
    noSkin: 'No skin set.',
    showCape: 'Show cape (applies to every skin here)',
    libraryHeading: 'My skins',
    createNew: 'Create new skin',
    libraryEmpty: 'No skins created yet.',
    use: 'Use',
    edit: 'Edit',
    pageOf: (current: number, total: number) => `Page ${current} / ${total}`,
    next: 'Next',
    uploadHeading: 'Upload skins from PC',
    variantClassic: 'Classic (Steve arms)',
    variantSlim: 'Slim (Alex arms)',
    uploading: 'Uploading…',
    selectAndUpload: 'Select PNG',
    capeHeading: 'Capes',
    capeDescription:
      'Your own, high-resolution capes, independent of Mojang\'s cape above. Visible to other players who have "Cape Provider" installed - toggle it on in this instance\'s Mods screen under "Bundled mods". Your collection stays on this PC, only the active cape is uploaded.',
    capeRequirements: 'PNG in 2:1 format, from 64x32 up to 2048x1024, at most 5 MB.',
    upload: 'Upload',
    selectCapePng: 'Add cape PNG',
    converterOpen: 'Convert image to cape',
    converterHeading: 'Convert image to cape',
    converterDescription:
      'Pick any image - it is placed on the outside of the cape, the side others see from behind. The preview above shows the result right away.',
    converterSelectImage: 'Select image',
    converterChangeImage: 'Different image',
    converterResolution: 'Resolution',
    converterFit: 'Fit',
    converterFitCover: 'Choose section',
    converterFitContain: 'Whole image (with border)',
    converterZoom: 'Section size',
    converterPipetteHint: 'Click into the image to use that color as the border color.',
    converterCropHint: 'Drag the frame with the mouse, make it bigger/smaller with the slider or the mouse wheel.',
    converterInside: 'Inside',
    converterInsideMirror: 'Mirrored image',
    converterInsideColor: 'Border color',
    converterPixelated: 'Sharp pixels (for pixel art)',
    converterBackground: 'Border color (edges, border, elytra)',
    converterApply: 'Apply',
    converterTooLarge: 'The result is larger than 5 MB - please choose a smaller resolution.',
    converterLoadFailed: 'The image could not be loaded.',
    converterDefaultName: (file: string) => `Cape from ${file}`,
    capeNamePlaceholder: "Cape's name",
    capeDefaultName: (date: string) => `Cape from ${date}`,
    capeSaveToCollection: 'Save to collection',
    capeCollectionHeading: 'My capes',
    capeCollectionEmpty: 'No capes saved yet.',
    capeActivate: 'Activate',
    capeDeactivate: 'Deactivate',
    capeDeactivating: 'Deactivating…',
    capeActivating: 'Activating…',
    capeActive: 'Active',
    capeNoActive: 'No custom cape active.',
    capeRemoveActive: 'Deactivate cape',
    capePreviewHint: 'Click a cape to show it in the preview.',
    deleteCapeConfirm: (name: string) => `Really delete "${name}" from your collection?`,
    deleteLibraryConfirm: (name: string) => `Really delete "${name}" from the library?`,
    removeCapeConfirm: 'The active cape is not in your collection on this PC - after deactivating it, it is gone. Deactivate anyway?',
    namePlaceholder: "Skin's name",
    saving: 'Saving…'
  },
  capeEditor: {
    title: 'Cape editor',
    open: 'Draw cape',
    edit: 'Edit',
    newHeading: 'New cape',
    loadHeading: 'Edit a cape from your PC',
    loadHint: 'Open a cape PNG from your PC and keep editing it here - 2:1, from 64x32 up to 2048x1024. It is saved as a new cape in your collection.',
    loadFromPc: 'Load cape PNG from PC',
    resolution: 'Resolution',
    baseColor: 'Base color',
    start: 'Start',
    toolFill: 'Fill',
    brushSize: 'Brush size',
    panels: {
      outside: 'Outside',
      inside: 'Inside',
      edgeTop: 'Top edge',
      edgeBottom: 'Bottom edge',
      edgeLeft: 'Left edge',
      edgeRight: 'Right edge',
      elytra: 'Elytra'
    },
    regionsHint:
      'Outside = the side others see from behind, with its four edges around it. Inside faces your back, elytra is used while wearing an elytra. Painting always stays on the grid you start on.',
    mirrorOutside: 'Mirror outside → inside',
    saveUpdate: 'Save changes',
    reactivateHint: 'If this cape is currently active, click "Activate" again afterwards so others see the new version.'
  },
  skinEditor: {
    defaultName: (date: string) => `Skin from ${date}`,
    chooserTitle: 'Create new skin',
    chooseSourceHeading: 'What should this start from?',
    loadSteveTemplate: 'Load Steve template',
    loadAlexTemplate: 'Load Alex template',
    loadOwnPng: 'Load your own PNG…',
    title: 'Skin editor',
    toolHeading: 'Tool',
    toolPencil: 'Pencil',
    toolEraser: 'Eraser',
    toolEyedropper: 'Eyedropper',
    toolView: 'View',
    undo: 'Undo',
    redo: 'Redo',
    showGrid: 'Show pixel grid',
    widenWindowHint:
      'Tip: Make the launcher window wider - then the tools and color palette sit next to the canvas and you no longer have to scroll up and down to change color.',
    visibilityHeading: 'Visibility',
    visibilityHint: 'Click a body part to show/hide it.',
    layerBase: 'Base',
    layerOverlay: 'Overlay',
    bodyParts: {
      head: 'Head',
      body: 'Body',
      rightArm: 'Right arm',
      leftArm: 'Left arm',
      rightLeg: 'Right leg',
      leftLeg: 'Left leg'
    },
    saveHeading: 'Save',
    updateInLibrary: 'Update in library',
    saveToLibrary: 'Save to library',
    exportPng: 'Export as PNG…'
  },
  credits: {
    title: 'Third-party credits'
  },
  console: {
    title: 'Console'
  },
  themePreview: {
    playerName: 'Player name',
    exampleInstance: 'Example instance (1.21.11)',
    sampleLogInfo: '[Launcher] Sample text for a readability check.',
    sampleLogError: '[Launcher] Sample error text.'
  },
  settings: {
    title: 'Settings',
    back: 'Back',
    memory: {
      heading: 'Memory (RAM)',
      auto: 'Automatic (Java default)',
      maxLabel: 'Max:',
      unit: 'MB',
      systemDetected: (size: string) => `${size} of system RAM detected`
    },
    storage: {
      heading: 'Storage location',
      loading: 'Loading…',
      free: (size: string) => `${size} free`,
      change: 'Change…'
    },
    instances: {
      heading: 'Instances',
      showSnapshots: 'Show snapshots when picking a version',
      showClientSupportMarks: 'Show the client logo in front of instances and versions the client supports'
    },
    console: {
      heading: 'Console',
      separateWindow: 'Show the console in a separate window on launch'
    },
    appearance: {
      heading: 'Appearance',
      description:
        "Two background colors, four accent tones and one text color for the existing launcher UI - a full choice between Minecraft's default look and a custom design (in-game as well as in the launcher) is a bigger, still-open item.",
      fields: {
        background1: 'Background 1',
        background2: 'Background 2',
        accent1: 'Accent 1',
        accent2: 'Accent 2',
        accent3: 'Accent 3',
        accent4: 'Accent 4',
        text: 'Text'
      },
      change: 'Change…',
      resetToDefault: 'Reset to default',
      resetConfirm: 'Reset all colors (backgrounds, accent tones, text) to the default? Your own customizations will be lost.',
      editField: (label: string) => `Edit ${label}`,
      contrastLabel: 'Contrast',
      contrastExplain: 'Contrast between the text color and the worst-affected background/accent color.',
      contrastBad: 'hard to read!',
      contrastBorderline: 'could be tight',
      contrastGood: 'easy to read',
      previewHeading: 'Preview',
      previewDescription: 'This is what your client will look like once you apply this color.',
      apply: 'Apply',
      cancel: 'Cancel'
    },
    language: {
      heading: 'Language',
      german: 'German',
      english: 'English'
    },
    clientDesign: {
      heading: 'Client design',
      description:
        'How the title screen and the Client Mods menu look in-game. You can also switch in-game: via the "Client Design" button in the Minecraft design, via the logo in the client design.',
      minecraft: 'Minecraft design',
      client: 'Client design (your colors)'
    },
    tour: {
      heading: 'Guided tour',
      description: 'Shows you every area of the launcher, step by step.',
      start: 'Start tour'
    },
    about: {
      heading: 'About',
      version: (version: string) => `Launcher version ${version}`
    }
  },
  errors: {
    auth: {
      xboxLiveFailed: (p: { status: number | string; detail: string }) => `Xbox Live authentication failed: ${p.status} ${p.detail}`,
      noXboxProfile: 'This Microsoft account has no Xbox Live profile. Create one on xbox.com and try again.',
      childAccountNoConsent: "This account is a child account without a parent/guardian's consent for Xbox Live.",
      xstsFailed: (p: { status: number | string; detail: string }) => `XSTS authorization failed: ${p.status} ${p.detail}`,
      msTokenExchangeFailed: (p: { status: number | string; detail: string }) =>
        `Microsoft token exchange failed: ${p.status} ${p.detail}`,
      loginTimeout: 'Microsoft login timed out after 5 minutes.',
      loopbackServerFailed: 'Failed to start the local server.',
      minecraftApiFailed: (p: { status: number | string; detail: string }) => `Minecraft API call failed (${p.status}): ${p.detail}`,
      minecraftRateLimited: 'Mojang is not accepting another login right now (too many logins in a short time). Please wait a few minutes and try again.'
    },
    image: {
      invalidPng: 'File is not a valid PNG.'
    },
    cape: {
      wrongDimensions: (p: { actualWidth: number | string; actualHeight: number | string }) =>
        `Capes must be 2:1 between 64x32 and 2048x1024 (64x32, 128x64, 256x128, …), this file is ${p.actualWidth}x${p.actualHeight}.`,
      tooLarge: (p: { maxMb: number }) => `The cape is too large - at most ${p.maxMb} MB.`,
      unauthorized: 'Your login has expired - please log out and back in to the launcher.',
      rateLimited: 'Too many cape changes in a short time - please wait a few minutes.',
      storageFull: "The client's server has no room for this right now. Try again later.",
      banned: 'This account may no longer upload custom capes because an earlier cape broke the rules.',
      libraryEntryNotFound: 'This cape is no longer in your collection.',
      statusLoadFailed: (p: { status: number | string }) => `Could not load cape status (${p.status}).`,
      uploadFailed: (p: { status: number | string; detail: string }) => `Cape upload failed (${p.status}): ${p.detail}`,
      deleteFailed: (p: { status: number | string; detail: string }) => `Removing the cape failed (${p.status}): ${p.detail}`
    },
    capeReport: {
      noPlayer: 'There is no player with that name.',
      noCape: "This player is not wearing a custom cape of the client right now.",
      self: "You can't report your own cape.",
      rateLimited: 'Too many reports in a short time - please try again later.',
      failed: (p: { detail: string }) => `That did not work (${p.detail}).`
    },
    skin: {
      wrongDimensions: (p: { width: number; height: number }) =>
        `Minecraft skins must be 64x64 (or the old 64x32 format), this file is ${p.width}x${p.height}.`,
      needInstanceFirst: 'Start an instance first (click Play) to load the Steve/Alex template.',
      templateEntryNotFound: (p: { entryPath: string; jarPath: string }) =>
        `Could not find "${p.entryPath}" in ${p.jarPath} - the path likely changed with a newer Minecraft version.`
    },
    download: {
      failed: (p: { status: number | string; url: string }) => `Download failed (${p.status}): ${p.url}`,
      sha1Mismatch: (p: { label: string; expected: string; actual: string }) =>
        `SHA-1 mismatch for ${p.label}: expected ${p.expected}, got ${p.actual}`
    },
    launcher: {
      busy: 'Game data is currently being moved or installed — please wait a moment.'
    },
    instance: {
      unknown: (p: { instanceId: string }) => `Unknown instance: ${p.instanceId}`,
      notFound: (p: { instanceId: string }) => `Instance ${p.instanceId} not found.`,
      importing: 'This instance is still being imported from another client - please wait until copying has finished.'
    },
    friends: {
      not_logged_in: 'Not logged in.',
      unauthorized: 'Your login has expired - please log out and in again.',
      unreachable: "Can't reach the friends server. It keeps retrying automatically.",
      auth_unavailable: "Your login couldn't be checked right now (Mojang unreachable). Try again in a moment.",
      multiplayer_blocked:
        'Your Minecraft account is not allowed to play multiplayer (switched off in the Xbox settings or banned by Mojang). Friends and custom capes need that permission, because it is how Mojang confirms who you are.',
      rate_limited: 'Too many requests - wait a moment and try again.',
      invalid_name: "That's not a valid Minecraft name.",
      player_not_found: 'No player with that name has used the client yet.',
      cannot_add_self: "You can't add yourself.",
      already_friends: "You're already friends.",
      already_requested: 'You already sent this player a request.',
      too_many_friends: 'Your friends list is full (200 friends at most).',
      too_many_requests: 'Too many open requests (50 at most) - wait until some are answered.',
      request_not_found: 'This request no longer exists.',
      not_friends: "You're not (or no longer) friends.",
      unblock_first: 'You have blocked this player - unblock them first.',
      too_many_blocked: 'Too many blocked players (500 at most).',
      invalid_body: 'Invalid request to the friends server.',
      unknown: 'Unknown error from the friends server.'
    },
    worlds: {
      noWorldFound: 'No Minecraft world found in your selection (a world is a folder with a level.dat inside).',
      deleteFailed: (p: { world: string }) =>
        `World "${p.world}" could not be removed. Is it open in the game right now? Close the game or the world first.`,
      zipUnreadable: (p: { file: string }) => `"${p.file}" is not a valid ZIP file or is damaged.`
    },
    modBundle: {
      manifestLoadFailed: (p: { status: number | string }) => `Could not load mod bundle manifest (${p.status}).`
    },
    launch: {
      assetIndexFetchFailed: (p: { status: number | string }) => `Failed to fetch asset index: ${p.status}`,
      noJavaRuntimeForPlatform: (p: { platform: string; arch: string }) =>
        `No Mojang Java runtime available for ${p.platform}/${p.arch}.`,
      javaManifestFetchFailed: (p: { status: number | string }) => `Failed to fetch Java runtime manifest: ${p.status}`,
      noJavaRuntimeForComponent: (p: { component: string; osKey: string }) =>
        `No Java runtime "${p.component}" available for ${p.osKey}.`,
      javaFileListFetchFailed: (p: { status: number | string }) => `Failed to fetch Java runtime file list: ${p.status}`,
      fabricGameVersionsFetchFailed: (p: { status: number | string }) => `Failed to fetch Fabric game versions: ${p.status}`,
      fabricLoaderVersionsFetchFailed: (p: { gameVersion: string; status: number | string }) =>
        `Failed to fetch Fabric loader versions for ${p.gameVersion}: ${p.status}`,
      noFabricLoaderVersion: (p: { gameVersion: string }) => `No Fabric loader version available for Minecraft ${p.gameVersion}.`,
      fabricProfileFetchFailed: (p: { gameVersion: string; loaderVersion: string; status: number | string }) =>
        `Failed to fetch Fabric profile for ${p.gameVersion}/${p.loaderVersion}: ${p.status}`,
      versionManifestFetchFailed: (p: { status: number | string }) => `Failed to fetch version manifest: ${p.status}`,
      versionNotFound: (p: { versionId: string }) => `Minecraft version ${p.versionId} not found in version manifest.`,
      versionDetailFetchFailed: (p: { versionId: string; status: number | string }) =>
        `Failed to fetch version detail for ${p.versionId}: ${p.status}`,
      cancelled: 'Launch cancelled.',
      offlineNotReady:
        "No internet connection, and this instance is still missing files. Launching offline only works after the instance was started online once."
    },
    mods: {
      searchFailed: (p: { status: number | string }) => `Modrinth search failed (${p.status})`,
      versionsLoadFailed: (p: { status: number | string }) => `Could not load Modrinth versions (${p.status})`,
      hashLookupFailed: (p: { status: number | string }) => `Modrinth hash lookup failed (${p.status})`,
      noFabricBuild: (p: { gameVersion: string; dependencyTitle?: string }) =>
        p.dependencyTitle
          ? `Dependency "${p.dependencyTitle}": no matching Fabric build found for Minecraft ${p.gameVersion}.`
          : `No matching Fabric build found for Minecraft ${p.gameVersion}.`,
      incompatibleWithInstalled: (p: { mod: string; installedMod: string }) =>
        `"${p.mod}" is not compatible with the already installed mod "${p.installedMod}" and can therefore not be installed.`,
      incompatibleWithEachOther: (p: { modA: string; modB: string }) =>
        `"${p.modA}" is not compatible with "${p.modB}" - both would be part of this installation, which isn't possible.`,
      noDownloadableFile: 'A required Modrinth version has no downloadable file.',
      modrinthVersionLoadFailed: (p: { versionId: string; status: number | string }) =>
        `Could not load Modrinth version ${p.versionId} (${p.status}).`,
      modrinthVersionNoFile: (p: { versionId: string }) => `Modrinth version ${p.versionId} has no downloadable file.`
    },
    storage: {
      targetInsideSource: 'The new location must not be inside the current location.'
    }
  }
}
