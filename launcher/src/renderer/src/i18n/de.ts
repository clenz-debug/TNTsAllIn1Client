/**
 * German strings - the launcher's original, still-default language. Every screen is migrated now
 * (own wishlist item: "dann würde ich gerne direkt den Rest machen") - going forward, any new
 * user-facing string needs an entry here *and* in `en.ts` in the same change, not just here.
 *
 * `en.ts` is typed against this file's own shape (`typeof de`) - adding a key here without adding
 * it there is a compile error, not a silent missing-translation bug.
 */
export const de = {
  common: {
    back: 'Zurück',
    cancel: 'Abbrechen',
    save: 'Speichern',
    delete: 'Löschen',
    remove: 'Entfernen',
    loading: 'Lädt…',
    hide: 'Ausblenden'
  },
  app: {
    checkingSession: 'Sitzung wird geprüft…'
  },
  login: {
    title: "TNT's All-In-1 Client",
    subtitle: 'Mit deinem Microsoft-Account anmelden, um zu spielen.',
    loginButton: 'Mit Microsoft anmelden',
    playOffline: (p: { name: string }) => `Offline spielen als ${p.name}`
  },
  // Own wishlist item ("client setup beim ersten start") - OnboardingScreen.tsx's colors step, the
  // only onboarding step that runs after the language is already picked (welcome/language stay
  // hardcoded-bilingual instead, see that file's own doc comment for why).
  onboarding: {
    colorsHeading: 'Passe die Farben an',
    colorsExplanation:
      'Du kannst die Farben des Launchers ganz nach deinem Geschmack anpassen - das wirkt sich auf den gesamten Client aus. Du kannst diesen Schritt auch überspringen und die Standardfarben behalten.',
    skip: 'Überspringen (Standardfarben)',
    next: 'Weiter',
    storageHeading: 'Wähle den Speicherort',
    storageExplanation:
      'Hier speichert der Launcher Minecraft, deine Instanzen und deine Welten - das können mit der Zeit viele Gigabyte werden. Egal, wo du den Launcher installiert hast: Erst einmal liegt das in deinem Benutzerordner (siehe unten). Ist dort wenig Platz, wähle am besten einen eigenen, leeren Ordner auf einer anderen Festplatte. Du kannst das auch später in den Einstellungen ändern.',
    storageMoving: 'Wird verschoben…',
    designHeading: 'Wähle das Design im Spiel',
    designExplanation:
      'Der Startbildschirm und das Client-Mods-Menü können wie normales Minecraft aussehen oder im eigenen Client-Design in deinen Farben. Du kannst das jederzeit in den Einstellungen oder direkt im Spiel auf dem Startbildschirm ändern.'
  },
  // Guided tour through every launcher area (own user request) - see tour/tourSteps.ts for the order.
  tour: {
    questionTitle: 'Einführungstour',
    questionText:
      'Willst du eine Einführungstour durch den Client machen, um ihn und alle seine Bereiche kennenzulernen? Das dauert nur ein paar Minuten.',
    questionLater: 'Du kannst die Tour auch später jederzeit in den Einstellungen starten.',
    yes: 'Ja, Tour starten',
    no: 'Nein, danke',
    counter: (current: number, total: number) => `Schritt ${current} von ${total}`,
    back: 'Zurück',
    next: 'Weiter',
    finish: 'Fertig',
    end: 'Tour beenden',
    waitingForInstance: 'Erstelle eine Instanz, um weiterzumachen.',
    instanceCreated: 'Super, deine Instanz ist erstellt!',
    skipWaiting: 'Ohne Erstellen weiter',
    inGameQuestion:
      'Willst du dir auch die Menüs im Spiel zeigen lassen? Dann startet jetzt die gewählte Instanz, und die Tour geht im Spiel weiter.',
    inGameYes: 'Ja, Spiel starten',
    inGameNo: 'Nein, fertig',
    steps: {
      account: {
        title: 'Dein Konto',
        text: 'Oben links siehst du, mit welchem Minecraft-Konto du angemeldet bist - mit deinem Namen und dem Kopf deines Skins. Ein Klick darauf öffnet ein kleines Menü, über das du dich abmelden kannst. Während der Tour ist das gesperrt.'
      },
      settingsButton: {
        title: 'Einstellungen',
        text: 'Über „Einstellungen“ passt du den Launcher an dich an. Schauen wir kurz hinein.'
      },
      settingsMemory: {
        title: 'Arbeitsspeicher und Speicherort',
        text: 'Hier legst du fest, wie viel Arbeitsspeicher (RAM) Minecraft bekommen darf - „Automatisch“ passt für die meisten. Darunter siehst du, wo Minecraft, deine Instanzen und deine Welten gespeichert werden, und kannst alles auf eine andere Festplatte verschieben.'
      },
      settingsGeneral: {
        title: 'Snapshots und Konsole',
        text: '„Snapshots anzeigen“ blendet Test-Versionen von Minecraft in der Versionsauswahl ein. Die Konsole zeigt beim Spielstart, was gerade passiert - auf Wunsch in einem eigenen Fenster statt hier im Launcher.'
      },
      settingsAppearance: {
        title: 'Erscheinungsbild',
        text: 'Hier änderst du die Farben des Launchers - dieselben, die du am Anfang wählen konntest. Die Vorschau zeigt dir jede Farbe, bevor du sie übernimmst.'
      },
      settingsLanguage: {
        title: 'Sprache, Design und diese Tour',
        text: 'Hier wechselst du die Sprache und wählst, ob der Startbildschirm und das Client-Mods-Menü im Spiel wie normales Minecraft oder im Client-Design in deinen Farben aussehen. Und hier kannst du diese Tour jederzeit wieder starten.'
      },
      credits: {
        title: 'Credits',
        text: 'Unter „Credits“ findest du alle Mods, Texturepacks und Schriften, die der Client mitbringt - mit ihrer Lizenz und einem Link zu den Leuten, die sie gemacht haben.'
      },
      skinButton: {
        title: 'Skin',
        text: 'Unter „Skin“ verwaltest du das Aussehen deiner Spielfigur.'
      },
      skinCurrent: {
        title: 'Dein aktueller Skin',
        text: 'So sieht deine Figur gerade aus. Mit gedrückter Maustaste drehst du sie, mit dem Mausrad zoomst du.'
      },
      skinLibrary: {
        title: 'Meine Skins',
        text: 'Hier sammelst du deine Skins. „Neuen Skin erstellen“ öffnet den Skin-Editor - dort malst du direkt auf dem 3D-Modell. Mit „Verwenden“ wird ein Skin zu deinem echten Minecraft-Skin.'
      },
      skinUpload: {
        title: 'Skin vom PC hochladen',
        text: 'Hast du schon eine Skin-Datei (PNG)? Dann lade sie hier direkt hoch - sie landet dabei auch in deiner Sammlung.'
      },
      capesButton: {
        title: 'Capes',
        text: 'Unter „Capes“ gestaltest du deinen eigenen Umhang. Den sehen alle anderen Spieler, die ebenfalls diesen Client benutzen.'
      },
      capesCreate: {
        title: 'Cape erstellen',
        text: 'Füge ein fertiges Cape-Bild hinzu, verwandle ein beliebiges Bild mit „Bild zu Cape umwandeln“ in ein Cape oder male eins mit „Cape zeichnen“. Die Vorschau zeigt es direkt an deiner Figur.'
      },
      capesCollection: {
        title: 'Meine Capes',
        text: 'Alle deine Capes landen hier. Mit „Aktivieren“ trägst du ein Cape im Spiel - du kannst es jederzeit wieder ablegen oder bearbeiten.'
      },
      capesRules: {
        title: 'Regeln für Capes',
        text: 'Dein Cape sehen auch andere Spieler. Hier steht, was nicht erlaubt ist - wer sich nicht daran hält, verliert sein Cape und darf keine eigenen Capes mehr hochladen.'
      },
      capesReport: {
        title: 'Cape melden',
        text: 'Trägt jemand ein Cape, das gegen die Regeln verstößt? Gib hier seinen Minecraft-Namen ein. Das Team des Clients sieht sich das Cape an; der Spieler erfährt nicht, wer ihn gemeldet hat.'
      },
      friendsButton: {
        title: 'Freunde',
        text: 'Unter „Freunde“ siehst du, wer von deinen Freunden gerade online ist. Eine Zahl dahinter zeigt offene Freundschaftsanfragen.'
      },
      friendsOwn: {
        title: 'Dein Status',
        text: 'Wähle, wie andere dich sehen: Online, Abwesend, Nicht stören oder Unsichtbar. Du kannst auch verbergen, auf welchem Server du gerade spielst.'
      },
      friendsAdd: {
        title: 'Freund hinzufügen',
        text: 'Gib den Minecraft-Namen eines Freundes ein und schick ihm eine Anfrage. Das geht bei allen, die den Client schon einmal benutzt haben.'
      },
      friendsList: {
        title: 'Deine Freunde',
        text: 'Hier siehst du deine Freunde und was sie gerade spielen. Ist jemand auf einem Server, kommst du mit „Beitreten“ direkt nach. Lädt dich ein Freund in seine Welt ein, erscheint oben ein Hinweis.'
      },
      instancesButton: {
        title: 'Instanzen',
        text: 'Eine Instanz ist eine eigene Minecraft-Installation mit eigener Version, eigenen Mods und eigenen Welten. So kannst du z. B. dieselbe Version einmal mit und einmal ohne bestimmte Mods haben.'
      },
      instancesCreate: {
        title: 'Instanz erstellen',
        text: 'Probier es gleich aus: Gib einen Namen ein, wähle eine Minecraft-Version und klicke auf „Erstellen“.'
      },
      instancesList: {
        title: 'Deine Instanzen',
        text: 'Hier stehen alle deine Instanzen. Du kannst sie auswählen, umbenennen, duplizieren oder löschen. Mit „Von anderem Client übernehmen…“ oben holst du Mods und Einstellungen aus einem anderen Launcher.'
      },
      instancePicker: {
        title: 'Instanz auswählen',
        text: 'Hier wählst du, welche Instanz startet, wenn du auf „Spielen“ klickst. Mods, Welten und Texturepacks gehören immer zur hier gewählten Instanz.'
      },
      modsButton: {
        title: 'Mods',
        text: 'Unter „Mods“ verwaltest du die Mods der gewählten Instanz.'
      },
      modsBundled: {
        title: 'Gebündelte Mods',
        text: 'Diese Mods bringt der Client selbst mit, zum Beispiel Sodium und Lithium für mehr FPS. Die meisten laufen immer mit - was sich abschalten lässt, schaltest du hier um.'
      },
      modsSearch: {
        title: 'Mods durchsuchen',
        text: 'Hier stöberst du durch tausende Mods von Modrinth. „Installieren“ lädt automatisch die passende Version für deine Instanz herunter.'
      },
      modsLegacy: {
        title: 'Mods in dieser Version',
        text: 'Minecraft-Versionen vor 1.14 starten ohne externe Mods - hier lässt sich deshalb nichts hinzufügen. Gibt es die Client Mods für die Version (zum Beispiel 1.8.9), findest du sie im Spiel unter „Client Mods“.'
      },
      modsOwn: {
        title: 'Eigene Mods',
        text: 'Hast du eine Mod-Datei (.jar) schon auf dem PC, fügst du sie hier hinzu. Jede Mod kannst du einzeln aus- und einschalten oder entfernen.'
      },
      worldsButton: {
        title: 'Welten',
        text: 'Unter „Welten“ findest du die Einzelspielerwelten der gewählten Instanz.'
      },
      worlds: {
        title: 'Welten verwalten',
        text: 'Lade Welten von deinem PC hoch - als Ordner oder ZIP, zum Beispiel aus dem normalen Minecraft-Launcher. Welten kannst du in eine andere Instanz kopieren oder verschieben und entfernen (sie landen dann im Papierkorb).'
      },
      resourcepacksButton: {
        title: 'Texturepacks',
        text: 'Unter „Texturepacks“ verwaltest du die Texturepacks der gewählten Instanz.'
      },
      resourcepacks: {
        title: 'Texturepacks verwalten',
        text: 'Lade eigene Texturepacks hoch oder entferne sie. Aktivieren kannst du sie danach im Spiel unter Optionen → Ressourcenpakete. Die Packs, die der Client mitbringt, verwaltet er selbst - die stehen hier nicht.'
      },
      play: {
        title: 'Los geht’s!',
        text: 'Mit „Spielen“ startest du die gewählte Instanz. Beim ersten Start lädt der Launcher Minecraft herunter, das kann ein paar Minuten dauern. Viel Spaß!'
      }
    }
  },
  play: {
    headerSkin: 'Skin',
    headerCapes: 'Capes',
    headerCredits: 'Credits',
    headerSettings: 'Einstellungen',
    headerLogout: 'Abmelden',
    playerMenuLabel: 'Konto-Menü',
    update: {
      available: (version: string) => `Update gefunden (Version ${version}) - wird heruntergeladen…`,
      downloading: (percent: number) => `Update wird heruntergeladen… (${percent}%)`,
      downloaded: (version: string) => `Update heruntergeladen (Version ${version}) - bereit zum Installieren.`,
      restartNow: 'Jetzt neu starten'
    },
    offline: {
      banner:
        'Offline-Modus: keine Internetverbindung. Du kannst Einzelspieler und LAN spielen; Server, Discord-Anzeige, Skin-/Cape-Upload, Mod-Suche und Updates brauchen Internet.',
      reconnect: 'Erneut verbinden',
      reconnecting: 'Verbinde…',
      stillOffline: 'Immer noch keine Verbindung.'
    },
    modBundleUpdate: {
      available: (names: string) => `Neue Mod-Bundle-Version verfügbar (${names}).`,
      ownMod: 'eigener Mod',
      applying: 'Wird aktualisiert…',
      apply: 'Aktualisieren'
    },
    instanceLabel: 'Instanz',
    noInstance: 'Keine Instanz',
    manageInstances: 'Instanzen verwalten…',
    mods: 'Mods…',
    worlds: 'Welten…',
    resourcepacks: 'Texturepacks…',
    versionListError: (error: string) => `Versionsliste konnte nicht geladen werden: ${error}`,
    noInstanceWarning: 'Noch keine Instanz angelegt - über "Instanzen verwalten…" eine erstellen.',
    bundleIncompatibleWarning: (versionId: string) =>
      `${versionId} hat keine gebündelten Mods/Resourcepacks (Sodium, Lithium, eigener Client-Mod, …) — startet als reines Fabric+Vanilla ohne Mods.`,
    legacyWarning: (versionId: string) =>
      `${versionId} startet als reines Minecraft — ohne Mods und ohne die Client-Features.`,
    legacyClientNote: (versionId: string) => `${versionId} startet ohne externe Mods — die Client Mods sind aber verfügbar.`,
    play: 'Spielen',
    playing: 'Läuft…',
    cancel: 'Start abbrechen'
  },
  instances: {
    title: 'Instanzen',
    newInstanceHeading: 'Neue Instanz',
    nameLabel: 'Name:',
    defaultName: (n: number) => `Instanz ${n}`,
    create: 'Erstellen',
    importing: 'Übernimmt…',
    importFromClient: 'Von anderem Client übernehmen…',
    importedSettingsWarning:
      'Übernommene Einstellungen (options.txt) gelten für alle deine Instanzen, nicht nur die neue - diese Einstellungen sind in diesem Launcher bewusst über alle Instanzen hinweg geteilt.',
    importResultMods: (count: number) => `${count} Mod(s) übernommen`,
    importResultSkipped: (count: number) => `${count} übersprungen (schon im Client enthalten oder nicht kompatibel)`,
    importResultResourcepacks: (count: number) => `${count} Texturpaket(e) übernommen`,
    importResultWorlds: (count: number) => `${count} Welt(en) übernommen`,
    importResultOptions: 'Einstellungen importiert',
    importResultNone: 'Keine Einstellungen, Mods, Texturpakete oder Welten im gewählten Ordner gefunden.',
    versionListError: (error: string) => `Versionsliste konnte nicht geladen werden: ${error}`,
    existingHeading: 'Vorhandene Instanzen',
    active: 'Aktiv',
    clientSupported: 'Diese Version wird vom Client unterstützt',
    supersededVersionHint: (versionId: string, fixedVersion: string) =>
      `${versionId} ist eine fehlerhafte Version, die Mojang kurz darauf durch ${fixedVersion} ersetzt hat. Wir empfehlen, die Version ${fixedVersion} zu nehmen, da diese stabiler läuft und deshalb vom Client unterstützt wird.`,
    select: 'Auswählen',
    rename: 'Umbenennen',
    cloning: 'Dupliziert…',
    clone: 'Duplizieren',
    copySuffix: (name: string) => `${name} (Kopie)`,
    deleteConfirm: (name: string) =>
      `"${name}" wirklich löschen? Speicherstände, Einstellungen und Mods dieser Instanz gehen dabei unwiderruflich verloren.`,
    empty: 'Noch keine Instanz angelegt.'
  },
  mods: {
    title: 'Mods',
    bundledHeading: 'Gebündelte Mods',
    bundledInfo:
      'Standardmäßig aus - hier gezielt aktivieren. Fabric API und Sodium/Lithium (Performance, für gute Leistung auch auf schwächeren Geräten) sowie Continuity (hat sein eigenes An/Aus im Mod-Menü ingame) laufen immer mit und tauchen deshalb nicht als eigene Schalter auf.',
    bundleIncompatible: (versionId: string) =>
      `Wirkt sich aktuell nicht aus - ${versionId} hat kein Mod-Bundle, startet ohnehin ohne gebündelte Mods.`,
    legacyNoMods: (versionId: string) =>
      `${versionId} startet als reines Minecraft ohne Mod-Loader - Mods lassen sich für Versionen vor 1.14 hier nicht verwenden.`,
    legacyClientModsOnly: (versionId: string) =>
      `${versionId} startet ohne externe Mods - für Versionen vor 1.14 lassen sich hier keine hinzufügen. Die Client Mods sind aber verfügbar: Du findest sie im Spiel unter „Client Mods“.`,
    bundledSource:
      'Die gebündelten Mods lädt der Launcher direkt von Modrinth herunter - beim ersten Start einer Version braucht das eine Internetverbindung. Bei Problemen mit diesen Mods (z. B. Sodium) bitte bei uns melden, nicht bei deren Entwicklern: Sie geben für Clients wie diesen keinen Support.',
    bundledLoading: 'Gebündelte Mods werden von Modrinth geladen…',
    noToggleable: 'Aktuell nichts zum Umschalten - alle derzeit gebündelten Mods laufen immer mit (siehe Hinweis oben).',
    searchHeading: 'Mods durchsuchen & entdecken',
    searchUnavailable: 'Modrinth-Suche ist nur für Mod-Bundle-kompatible Versionen verfügbar.',
    searchPlaceholder: 'Mods durchsuchen (leer lassen zum Durchstöbern)…',
    searching: 'Sucht…',
    search: 'Suchen',
    alreadyBundled: 'Bereits gebündelt',
    installed: 'Installiert',
    installing: 'Wird installiert…',
    install: 'Installieren',
    noResults: 'Keine Mods gefunden.',
    ownHeading: 'Eigene Mods',
    addMod: 'Mod hinzufügen…',
    noneAdded: 'Keine eigenen Mods hinzugefügt.',
    notices: {
      essential:
        'Essential erkannt: Solange es installiert ist, läuft das Spiel im Minecraft-Design (Essentials Menüs lassen sich nicht anpassen), und Essentials Tasten werden beim ersten Start auf „nicht belegt“ gesetzt. Essential-Cosmetics können Client-Capes überdecken.',
      optifine:
        'OptiFine/OptiFabric ist nicht mit Sodium kompatibel - das Spiel startet damit nicht. Für Shader stattdessen Iris verwenden.'
    },
    sortOptions: {
      relevance: 'Relevanz',
      downloads: 'Downloads',
      follows: 'Follower',
      newest: 'Neueste',
      updated: 'Kürzlich aktualisiert'
    },
    sortAriaLabel: 'Sortierung',
    pagesAriaLabel: 'Seiten'
  },
  worlds: {
    title: 'Welten',
    heading: 'Welten dieser Instanz',
    empty: 'Keine Welten in dieser Instanz.',
    copy: 'Kopieren',
    move: 'Verschieben',
    needsAnotherInstance: 'Kopieren/Verschieben braucht mindestens eine weitere Instanz - lege dafür erst eine zweite an.',
    movedStatus: (world: string, target: string) => `Welt "${world}" nach "${target}" verschoben.`,
    copiedStatus: (world: string, target: string, copiedTo: string) =>
      `Welt "${world}" nach "${target}" kopiert (dort als "${copiedTo}").`,
    actionTitleMove: (world: string) => `Welt "${world}" verschieben`,
    actionTitleCopy: (world: string) => `Welt "${world}" kopieren`,
    targetInstanceLabel: 'Ziel-Instanz:',
    moveWarning:
      'Achtung: Unterschiedliche Minecraft-Versionen oder Mods zwischen den Instanzen können diese Welt beschädigen oder zum Absturz führen (z.B. fehlende Blöcke/Items aus Mods, die in der Zielinstanz nicht installiert sind, oder ein Chunk-Format, das eine ältere Version nicht laden kann). Das geschieht auf eigene Gefahr und kann danach nicht rückgängig gemacht werden.',
    confirm: 'Bestätigen',
    working: 'Wird ausgeführt…',
    uploadInfo:
      'Welten von deinem PC in diese Instanz übernehmen - als Ordner oder als ZIP. Wählst du einen ganzen saves-Ordner, werden alle Welten darin übernommen. Das Original bleibt unverändert.',
    uploadFolder: 'Weltordner hochladen…',
    uploadZip: 'Welt-ZIP hochladen…',
    uploadDialogFolder: 'Weltordner auswählen',
    uploadDialogZip: 'Welt-ZIP(s) auswählen',
    uploading: 'Wird hochgeladen…',
    actionTitleDelete: (world: string) => `Welt "${world}" entfernen`,
    deleteInfo: 'Die Welt wird in den Papierkorb verschoben - von dort kannst du sie bei Bedarf wiederherstellen.',
    deletedStatus: (world: string) => `Welt "${world}" in den Papierkorb verschoben.`,
    uploadedStatus: (worlds: string[]) =>
      worlds.length === 1 ? `Welt "${worlds[0]}" hochgeladen.` : `${worlds.length} Welten hochgeladen: ${worlds.join(', ')}.`
  },
  friends: {
    title: 'Freunde',
    headerButton: (incoming: number) => (incoming > 0 ? `Freunde (${incoming})` : 'Freunde'),
    enabledLabel: 'Freunde und Online-Status',
    enabledInfo:
      'Solange das an ist, meldet der Launcher alle 20 Sekunden deinen Status und was du gerade spielst an den Server des Clients und fragt dort ab, welche Mitspieler den Client nutzen (für das Logo an ihrem Namen). Ausgeschaltet nimmt der Launcher von sich aus keinen Kontakt zu diesem Server auf. Was genau gespeichert wird, steht in PRIVACY.md auf der GitHub-Seite des Projekts.',
    disabledInfo:
      'Freunde sind ausgeschaltet: kein Online-Status, keine Freundesliste, keine Einladungen und keine Client-Logos im Spiel. Deine Freunde bleiben gespeichert und sind wieder da, sobald du es einschaltest.',
    ownHeading: 'Dein Status',
    statusLabel: 'Status:',
    ownStatus: { online: 'Online', away: 'Abwesend', dnd: 'Nicht stören', invisible: 'Unsichtbar' },
    visibleStatus: { online: 'Online', away: 'Abwesend', dnd: 'Nicht stören', offline: 'Offline' },
    hideServer: 'Server vor Freunden verbergen',
    addHeading: 'Freund hinzufügen',
    addInfo: 'Gib den Minecraft-Namen ein. Hinzufügen kannst du nur Spieler, die den Client schon einmal benutzt haben.',
    addPlaceholder: 'Minecraft-Name',
    addButton: 'Anfrage senden',
    requestSent: (name: string) => `Anfrage an ${name} gesendet.`,
    nowFriends: (name: string) => `Du und ${name} seid jetzt befreundet.`,
    requestsHeading: 'Anfragen',
    incoming: 'Möchte mit dir befreundet sein',
    outgoing: 'Anfrage gesendet, wartet auf Antwort',
    accept: 'Annehmen',
    decline: 'Ablehnen',
    withdraw: 'Zurückziehen',
    friendsHeading: (count: number) => `Freunde (${count})`,
    empty: 'Noch keine Freunde - schick oben eine Anfrage.',
    confirmRemove: (name: string) => `${name} aus deiner Freundesliste entfernen?`,
    block: 'Blockieren',
    unblock: 'Freigeben',
    confirmBlock: (name: string) =>
      `${name} blockieren? Eine Freundschaft und offene Anfragen zwischen euch werden entfernt, und ${name} kann dir keine Anfragen mehr schicken. ${name} erfährt davon nichts.`,
    blockedHeading: (count: number) => `Blockiert (${count})`,
    deleteData: 'Meine Daten vom Server löschen…',
    confirmDeleteData:
      'Alles löschen, was der Server des Clients über dich gespeichert hat? Das sind deine Freunde, offene Anfragen, blockierte Spieler, dein Status und dein aktives Cape. Das lässt sich nicht rückgängig machen. „Freunde und Online-Status“ wird dabei ausgeschaltet - schaltest du es wieder ein, fängst du mit einer leeren Liste neu an. Deine Cape-Sammlung auf diesem PC bleibt.',
    deleteDataConfirm: 'Alles löschen',
    dataDeleted: 'Deine Daten auf dem Server sind gelöscht.',
    join: 'Beitreten',
    joinInGameHint: 'Das Spiel läuft schon - du wirst direkt dorthin verbunden.',
    inviteText: (name: string, version: string) => `${name} lädt dich in seine Welt ein (Minecraft ${version}).`,
    noInstanceForVersion: (version: string) => `Du brauchst eine Instanz mit Minecraft ${version} - leg unter "Instanzen" eine an.`,
    noInstance: 'Wähle zuerst eine Instanz aus.',
    activity: {
      launcher: 'Im Launcher',
      menu: 'Im Hauptmenü',
      singleplayer: 'Spielt Einzelspieler',
      multiplayer: 'Spielt Mehrspieler',
      playing: 'Spielt Minecraft',
      server: (server: string) => `Spielt auf ${server}`
    }
  },
  capeRules: {
    heading: 'Regeln für Capes',
    intro: 'Dein aktives Cape ist öffentlich: Jeder Spieler mit diesem Client sieht es an dir. Nicht erlaubt sind Capes mit:',
    items: [
      'Nacktheit, Pornografie oder anderen sexuellen Inhalten',
      'Hass, Beleidigungen oder Diskriminierung, etwa wegen Herkunft, Religion, Geschlecht oder Sexualität',
      'verbotenen Symbolen oder Verherrlichung von Gewalt',
      'allem, was gegen geltendes Recht verstößt'
    ],
    consequence:
      'Das Team des Clients kann ein Cape nach eigenem Ermessen entfernen und das Konto für weitere Uploads sperren - auch dann, wenn ein Cape keinen dieser Punkte trifft, aber nicht zum Client passt.'
  },
  capeReport: {
    heading: 'Cape melden',
    info: 'Trägt jemand ein eigenes Cape, das anstößig oder verboten ist? Gib den Minecraft-Namen an. Das Cape wird so gesichert, wie es gerade ist, und vom Team des Clients angesehen. Der gemeldete Spieler erfährt nicht, wer ihn gemeldet hat.',
    namePlaceholder: 'Minecraft-Name',
    reasonPlaceholder: 'Was ist das Problem? (freiwillig)',
    send: 'Melden',
    sent: (name: string) => `Das Cape von ${name} wurde gemeldet. Danke!`
  },
  moderation: {
    heading: (count: number) => `Gemeldete Capes (${count})`,
    info: 'Nur für dich als Moderator sichtbar. Gezeigt wird das Cape so, wie es gemeldet wurde.',
    refresh: 'Neu laden',
    empty: 'Keine offenen Meldungen.',
    reportCount: (count: number) => (count === 1 ? '1 Meldung' : `${count} Meldungen`),
    stillWorn: 'wird noch getragen',
    replaced: 'inzwischen ersetzt oder entfernt',
    dismiss: 'Verwerfen',
    remove: 'Cape entfernen',
    ban: 'Sperren',
    unban: 'Entsperren',
    confirmDismiss: (name: string) => `Die Meldungen zu ${name} verwerfen? Das Cape bleibt.`,
    confirmRemove: (name: string) => `Das Cape von ${name} entfernen? ${name} kann danach ein neues hochladen.`,
    confirmBan: (name: string) => `Das Cape von ${name} entfernen und das Konto für Uploads sperren? Der Grund wird gespeichert.`,
    banReasonPlaceholder: 'Grund der Sperre',
    bansHeading: (count: number) => `Gesperrte Konten (${count})`
  },
  resourcepacks: {
    title: 'Texturepacks',
    heading: (instance: string) => `Externe Texturepacks in "${instance}"`,
    info: 'Hier stehen nur deine eigenen Packs - die mitgelieferten des Clients werden automatisch verwaltet. Aktivieren kannst du ein Pack im Spiel unter Optionen → Ressourcenpakete.',
    add: 'Pack hochladen…',
    dialogTitle: 'Texturepack(s) auswählen',
    removeAll: 'Alle entfernen',
    empty: 'Noch keine eigenen Texturepacks in dieser Instanz.',
    confirmRemove: (name: string) => `Texturepack "${name}" wirklich aus dieser Instanz löschen?`,
    confirmRemoveAll: (count: number) =>
      `Wirklich alle ${count} eigenen Texturepacks aus dieser Instanz löschen? Die mitgelieferten Packs bleiben erhalten.`
  },
  skin: {
    renameTitle: 'Skin benennen',
    title: 'Skins',
    currentHeading: 'Aktueller Skin',
    noSkin: 'Kein Skin gesetzt.',
    showCape: 'Cape anzeigen (gilt für alle Skins hier)',
    libraryHeading: 'Meine Skins',
    createNew: 'Neuen Skin erstellen',
    libraryEmpty: 'Noch keine Skins erstellt.',
    use: 'Verwenden',
    edit: 'Bearbeiten',
    pageOf: (current: number, total: number) => `Seite ${current} / ${total}`,
    next: 'Weiter',
    uploadHeading: 'Skins vom PC hochladen',
    variantClassic: 'Classic (Steve-Arme)',
    variantSlim: 'Slim (Alex-Arme)',
    uploading: 'Lädt hoch…',
    selectAndUpload: 'PNG auswählen',
    capeHeading: 'Capes',
    capeDescription:
      'Eigene, hochauflösende Capes, unabhängig von Mojangs Cape oben. Sichtbar für andere Spieler, die "Cape Provider" installiert haben - im Mods-Bildschirm dieser Instanz unter "Gebündelte Mods" zuschaltbar. Deine Sammlung bleibt auf diesem PC, nur das aktive Cape wird hochgeladen.',
    capeRequirements: 'PNG im Format 2:1, von 64x32 bis 2048x1024, höchstens 5 MB.',
    upload: 'Hochladen',
    selectCapePng: 'Cape-PNG hinzufügen',
    converterOpen: 'Bild zu Cape umwandeln',
    converterHeading: 'Bild zu Cape umwandeln',
    converterDescription:
      'Beliebiges Bild auswählen - es wird auf die Außenseite des Capes gesetzt, also die Seite, die andere von hinten sehen. Die Vorschau oben zeigt das Ergebnis sofort.',
    converterSelectImage: 'Bild auswählen',
    converterChangeImage: 'Anderes Bild',
    converterResolution: 'Auflösung',
    converterFit: 'Anpassung',
    converterFitCover: 'Ausschnitt wählen',
    converterFitContain: 'Ganzes Bild (mit Rand)',
    converterZoom: 'Ausschnitt-Größe',
    converterPipetteHint: 'Klick ins Bild übernimmt die Farbe als Randfarbe.',
    converterCropHint: 'Rahmen mit der Maus verschieben, mit dem Regler oder dem Mausrad größer/kleiner machen.',
    converterInside: 'Innenseite',
    converterInsideMirror: 'Bild gespiegelt',
    converterInsideColor: 'Randfarbe',
    converterPixelated: 'Scharfe Pixel (für Pixel-Art)',
    converterBackground: 'Randfarbe (Kanten, Rand, Elytra)',
    converterApply: 'Übernehmen',
    converterTooLarge: 'Das Ergebnis ist größer als 5 MB - bitte eine kleinere Auflösung wählen.',
    converterLoadFailed: 'Das Bild konnte nicht geladen werden.',
    converterDefaultName: (file: string) => `Cape aus ${file}`,
    capeNamePlaceholder: 'Name des Capes',
    capeDefaultName: (date: string) => `Cape vom ${date}`,
    capeSaveToCollection: 'In Sammlung speichern',
    capeCollectionHeading: 'Meine Capes',
    capeCollectionEmpty: 'Noch keine Capes gespeichert.',
    capeActivate: 'Aktivieren',
    capeDeactivate: 'Deaktivieren',
    capeDeactivating: 'Wird deaktiviert…',
    capeActivating: 'Wird aktiviert…',
    capeActive: 'Aktiv',
    capeNoActive: 'Kein eigenes Cape aktiv.',
    capeRemoveActive: 'Cape deaktivieren',
    capePreviewHint: 'Klick auf ein Cape zeigt es in der Vorschau.',
    deleteCapeConfirm: (name: string) => `"${name}" wirklich aus deiner Sammlung löschen?`,
    deleteLibraryConfirm: (name: string) => `"${name}" wirklich aus der Bibliothek löschen?`,
    removeCapeConfirm:
      'Das aktive Cape ist nicht in deiner Sammlung auf diesem PC - nach dem Deaktivieren ist es weg. Trotzdem deaktivieren?',
    namePlaceholder: 'Name des Skins',
    saving: 'Speichert…'
  },
  capeEditor: {
    title: 'Cape-Editor',
    open: 'Cape zeichnen',
    edit: 'Bearbeiten',
    newHeading: 'Neues Cape',
    loadHeading: 'Cape vom PC bearbeiten',
    loadHint: 'Eine Cape-PNG von deinem PC öffnen und hier weiterbearbeiten - 2:1, von 64x32 bis 2048x1024. Gespeichert wird sie als neues Cape in deiner Sammlung.',
    loadFromPc: 'Cape-PNG vom PC laden',
    resolution: 'Auflösung',
    baseColor: 'Grundfarbe',
    start: 'Loslegen',
    toolFill: 'Füllen',
    brushSize: 'Stiftgröße',
    panels: {
      outside: 'Außen',
      inside: 'Innen',
      edgeTop: 'Rand oben',
      edgeBottom: 'Rand unten',
      edgeLeft: 'Rand links',
      edgeRight: 'Rand rechts',
      elytra: 'Elytra'
    },
    regionsHint:
      'Außen = die Seite, die andere von hinten sehen, mit ihren vier Rändern drumherum. Innen liegt am Rücken, Elytra wird beim Tragen einer Elytra benutzt. Gemalt wird immer nur auf dem Raster, auf dem du anfängst.',
    mirrorOutside: 'Außen → Innen spiegeln',
    saveUpdate: 'Änderungen speichern',
    reactivateHint: 'Ist dieses Cape gerade aktiv, danach erneut „Aktivieren“, damit andere die neue Version sehen.'
  },
  skinEditor: {
    defaultName: (date: string) => `Skin vom ${date}`,
    chooserTitle: 'Neuen Skin erstellen',
    chooseSourceHeading: 'Wovon soll gestartet werden?',
    loadSteveTemplate: 'Steve-Vorlage laden',
    loadAlexTemplate: 'Alex-Vorlage laden',
    loadOwnPng: 'Eigene PNG laden…',
    title: 'Skin-Editor',
    toolHeading: 'Werkzeug',
    toolPencil: 'Stift',
    toolEraser: 'Radierer',
    toolEyedropper: 'Pipette',
    toolView: 'Ansicht',
    undo: 'Rückgängig',
    redo: 'Wiederholen',
    showGrid: 'Pixel-Raster anzeigen',
    widenWindowHint:
      'Tipp: Zieh das Launcher-Fenster breiter - dann stehen Werkzeuge und Farbpalette neben der Zeichenfläche und du musst zum Farbwechsel nicht hoch- und runterscrollen.',
    visibilityHeading: 'Sichtbarkeit',
    visibilityHint: 'Auf ein Körperteil klicken, um es ein-/auszublenden.',
    layerBase: 'Basis',
    layerOverlay: 'Overlay',
    bodyParts: {
      head: 'Kopf',
      body: 'Körper',
      rightArm: 'Rechter Arm',
      leftArm: 'Linker Arm',
      rightLeg: 'Rechtes Bein',
      leftLeg: 'Linkes Bein'
    },
    saveHeading: 'Speichern',
    updateInLibrary: 'In Bibliothek aktualisieren',
    saveToLibrary: 'In Bibliothek speichern',
    exportPng: 'Als PNG exportieren…'
  },
  credits: {
    title: 'Drittanbieter-Credits'
  },
  console: {
    title: 'Konsole'
  },
  themePreview: {
    playerName: 'Spielername',
    exampleInstance: 'Beispiel-Instanz (1.21.11)',
    sampleLogInfo: '[Launcher] Beispieltext zur Lesbarkeitsprüfung.',
    sampleLogError: '[Launcher] Beispiel-Fehlertext.'
  },
  settings: {
    title: 'Einstellungen',
    back: 'Zurück',
    memory: {
      heading: 'Arbeitsspeicher (RAM)',
      auto: 'Automatisch (Java-Standard)',
      maxLabel: 'Max:',
      unit: 'MB',
      systemDetected: (size: string) => `${size} System-RAM erkannt`
    },
    storage: {
      heading: 'Speicherort',
      loading: 'Lädt…',
      free: (size: string) => `${size} frei`,
      change: 'Ändern…'
    },
    instances: {
      heading: 'Instanzen',
      showSnapshots: 'Snapshots bei der Versionsauswahl anzeigen',
      showClientSupportMarks: 'Client-Logo vor Instanzen und Versionen anzeigen, die der Client unterstützt'
    },
    console: {
      heading: 'Konsole',
      separateWindow: 'Konsole beim Start in einem separaten Fenster anzeigen'
    },
    appearance: {
      heading: 'Erscheinungsbild',
      description:
        'Zwei Hintergrundfarben, vier Akzentfarbtöne und eine Schriftfarbe für die bestehende Launcher-Oberfläche - eine vollständige Auswahl zwischen Minecraft-Standard-Design und eigenem Design (ingame wie im Launcher) ist ein größeres, noch offenes Vorhaben.',
      fields: {
        background1: 'Hintergrund 1',
        background2: 'Hintergrund 2',
        accent1: 'Akzent 1',
        accent2: 'Akzent 2',
        accent3: 'Akzent 3',
        accent4: 'Akzent 4',
        text: 'Schrift'
      },
      change: 'Ändern…',
      resetToDefault: 'Zum Standard zurücksetzen',
      resetConfirm:
        'Alle Farben (Hintergründe, Akzenttöne, Schrift) auf den Standard zurücksetzen? Eigene Anpassungen gehen dabei verloren.',
      editField: (label: string) => `${label} bearbeiten`,
      contrastLabel: 'Kontrast',
      contrastExplain: 'Kontrast zwischen der Schriftfarbe und dem ungünstigsten betroffenen Hintergrund/Akzent.',
      contrastBad: 'schwer lesbar!',
      contrastBorderline: 'könnte knapp sein',
      contrastGood: 'gut lesbar',
      previewHeading: 'Vorschau',
      previewDescription: 'So sieht dein Client aus, sobald du diese Farbe übernimmst.',
      apply: 'Übernehmen',
      cancel: 'Abbrechen'
    },
    language: {
      heading: 'Sprache',
      german: 'Deutsch',
      english: 'Englisch'
    },
    clientDesign: {
      heading: 'Client-Design',
      description:
        'Wie der Startbildschirm und das Client-Mods-Menü im Spiel aussehen. Umschalten geht auch im Spiel: im Minecraft-Design über den Button „Client-Design“, im Client-Design über das Logo.',
      minecraft: 'Minecraft-Design',
      client: 'Client-Design (deine Farben)'
    },
    tour: {
      heading: 'Einführungstour',
      description: 'Zeigt dir Schritt für Schritt alle Bereiche des Launchers.',
      start: 'Tour starten'
    },
    about: {
      heading: 'Über',
      version: (version: string) => `Launcher-Version ${version}`
    }
  },
  /**
   * Localized counterparts of the error codes `shared/errorMessages.ts#localizedError` encodes in
   * main-process throw sites - `formatError.ts` looks a code up here (dot-path, e.g.
   * `cape.notConfigured`) instead of showing whatever raw string the main process happened to throw,
   * so an error is in the user's chosen language regardless of which process detected it. Keys must
   * match the `code` string passed to `localizedError` at every call site 1:1 - a typo on either end
   * just falls back to the raw (German) message, same as any not-yet-migrated error.
   */
  errors: {
    auth: {
      xboxLiveFailed: (p: { status: number | string; detail: string }) =>
        `Xbox-Live-Authentifizierung fehlgeschlagen: ${p.status} ${p.detail}`,
      noXboxProfile: 'Dieses Microsoft-Konto hat kein Xbox-Live-Profil. Auf xbox.com eines anlegen und erneut versuchen.',
      childAccountNoConsent: 'Dieser Account ist ein Kinderkonto ohne Zustimmung eines Erziehungsberechtigten für Xbox Live.',
      xstsFailed: (p: { status: number | string; detail: string }) => `XSTS-Autorisierung fehlgeschlagen: ${p.status} ${p.detail}`,
      msTokenExchangeFailed: (p: { status: number | string; detail: string }) =>
        `Microsoft-Token-Austausch fehlgeschlagen: ${p.status} ${p.detail}`,
      loginTimeout: 'Microsoft-Anmeldung nach 5 Minuten abgelaufen.',
      loopbackServerFailed: 'Lokaler Server konnte nicht gestartet werden.',
      minecraftApiFailed: (p: { status: number | string; detail: string }) =>
        `Minecraft-API-Aufruf fehlgeschlagen (${p.status}): ${p.detail}`,
      minecraftRateLimited: 'Mojang lässt gerade keine weitere Anmeldung zu (zu viele Anmeldungen in kurzer Zeit). Bitte ein paar Minuten warten und es dann noch einmal versuchen.'
    },
    image: {
      invalidPng: 'Datei ist kein gültiges PNG.'
    },
    cape: {
      wrongDimensions: (p: { actualWidth: number | string; actualHeight: number | string }) =>
        `Capes müssen im Format 2:1 zwischen 64x32 und 2048x1024 sein (64x32, 128x64, 256x128, …), diese Datei ist ${p.actualWidth}x${p.actualHeight}.`,
      tooLarge: (p: { maxMb: number }) => `Das Cape ist zu groß - höchstens ${p.maxMb} MB.`,
      unauthorized: 'Deine Anmeldung ist abgelaufen - bitte im Launcher ab- und wieder anmelden.',
      rateLimited: 'Zu viele Cape-Änderungen in kurzer Zeit - bitte ein paar Minuten warten.',
      banned: 'Dieses Konto darf keine eigenen Capes mehr hochladen, weil ein früheres Cape gegen die Regeln verstoßen hat.',
      libraryEntryNotFound: 'Dieses Cape ist nicht mehr in deiner Sammlung.',
      statusLoadFailed: (p: { status: number | string }) => `Cape-Status konnte nicht geladen werden (${p.status}).`,
      uploadFailed: (p: { status: number | string; detail: string }) => `Cape-Upload fehlgeschlagen (${p.status}): ${p.detail}`,
      deleteFailed: (p: { status: number | string; detail: string }) => `Cape entfernen fehlgeschlagen (${p.status}): ${p.detail}`
    },
    capeReport: {
      noPlayer: 'Einen Spieler mit diesem Namen gibt es nicht.',
      noCape: 'Dieser Spieler trägt gerade kein eigenes Cape des Clients.',
      self: 'Du kannst dein eigenes Cape nicht melden.',
      rateLimited: 'Zu viele Meldungen in kurzer Zeit - bitte später nochmal.',
      failed: (p: { detail: string }) => `Das hat nicht geklappt (${p.detail}).`
    },
    skin: {
      wrongDimensions: (p: { width: number; height: number }) =>
        `Minecraft-Skins müssen 64x64 (oder das alte 64x32-Format) sein, diese Datei ist ${p.width}x${p.height}.`,
      needInstanceFirst: 'Erst eine Instanz starten (auf „Spielen“ klicken), um die Steve/Alex-Vorlage laden zu können.',
      templateEntryNotFound: (p: { entryPath: string; jarPath: string }) =>
        `Konnte "${p.entryPath}" nicht in ${p.jarPath} finden - der Pfad hat sich vermutlich mit einer neueren Minecraft-Version geändert.`
    },
    download: {
      failed: (p: { status: number | string; url: string }) => `Download fehlgeschlagen (${p.status}): ${p.url}`,
      sha1Mismatch: (p: { label: string; expected: string; actual: string }) =>
        `SHA-1 stimmt nicht überein für ${p.label}: erwartet ${p.expected}, erhalten ${p.actual}`
    },
    launcher: {
      busy: 'Spieldaten werden gerade verschoben oder installiert — bitte kurz warten.'
    },
    instance: {
      unknown: (p: { instanceId: string }) => `Unbekannte Instanz: ${p.instanceId}`,
      notFound: (p: { instanceId: string }) => `Instanz ${p.instanceId} nicht gefunden.`,
      importing: 'Diese Instanz wird noch von einem anderen Client übernommen - bitte warten, bis das Kopieren fertig ist.'
    },
    friends: {
      not_logged_in: 'Nicht angemeldet.',
      unauthorized: 'Deine Anmeldung ist abgelaufen - bitte ab- und wieder anmelden.',
      unreachable: 'Freunde-Server nicht erreichbar. Es wird automatisch weiter versucht.',
      auth_unavailable: 'Die Anmeldung konnte gerade nicht geprüft werden (Mojang nicht erreichbar). Versuch es gleich nochmal.',
      multiplayer_blocked:
        'Dein Minecraft-Konto darf nicht im Mehrspielermodus spielen (in den Xbox-Einstellungen abgeschaltet oder von Mojang gesperrt). Freunde und eigene Capes brauchen diese Freigabe, weil Mojang darüber bestätigt, wer du bist.',
      rate_limited: 'Zu viele Anfragen - warte kurz und versuch es dann nochmal.',
      invalid_name: 'Das ist kein gültiger Minecraft-Name.',
      player_not_found: 'Kein Spieler mit diesem Namen hat den Client bisher benutzt.',
      cannot_add_self: 'Du kannst dich nicht selbst hinzufügen.',
      already_friends: 'Ihr seid bereits befreundet.',
      already_requested: 'Du hast diesem Spieler schon eine Anfrage geschickt.',
      too_many_friends: 'Die Freundesliste ist voll (höchstens 200 Freunde).',
      too_many_requests: 'Zu viele offene Anfragen (höchstens 50) - warte, bis einige beantwortet sind.',
      request_not_found: 'Diese Anfrage gibt es nicht mehr.',
      not_friends: 'Ihr seid nicht (mehr) befreundet.',
      unblock_first: 'Du hast diesen Spieler blockiert - gib ihn erst wieder frei.',
      too_many_blocked: 'Zu viele blockierte Spieler (höchstens 500).',
      invalid_body: 'Ungültige Anfrage an den Freunde-Server.',
      unknown: 'Unbekannter Fehler beim Freunde-Server.'
    },
    worlds: {
      noWorldFound: 'In der Auswahl wurde keine Minecraft-Welt gefunden (eine Welt ist ein Ordner mit einer level.dat darin).',
      deleteFailed: (p: { world: string }) =>
        `Welt "${p.world}" konnte nicht entfernt werden. Ist sie gerade im Spiel geöffnet? Dann erst das Spiel bzw. die Welt schließen.`,
      zipUnreadable: (p: { file: string }) => `"${p.file}" ist keine gültige oder eine beschädigte ZIP-Datei.`
    },
    modBundle: {
      manifestLoadFailed: (p: { status: number | string }) => `Mod-Bundle-Manifest konnte nicht geladen werden (${p.status}).`
    },
    launch: {
      assetIndexFetchFailed: (p: { status: number | string }) => `Asset-Index konnte nicht geladen werden: ${p.status}`,
      noJavaRuntimeForPlatform: (p: { platform: string; arch: string }) =>
        `Kein Mojang-Java-Runtime für ${p.platform}/${p.arch} verfügbar.`,
      javaManifestFetchFailed: (p: { status: number | string }) => `Java-Runtime-Manifest konnte nicht geladen werden: ${p.status}`,
      noJavaRuntimeForComponent: (p: { component: string; osKey: string }) =>
        `Kein Java-Runtime "${p.component}" für ${p.osKey} verfügbar.`,
      javaFileListFetchFailed: (p: { status: number | string }) => `Java-Runtime-Dateiliste konnte nicht geladen werden: ${p.status}`,
      fabricGameVersionsFetchFailed: (p: { status: number | string }) =>
        `Fabric-Spielversionen konnten nicht geladen werden: ${p.status}`,
      fabricLoaderVersionsFetchFailed: (p: { gameVersion: string; status: number | string }) =>
        `Fabric-Loader-Versionen für ${p.gameVersion} konnten nicht geladen werden: ${p.status}`,
      noFabricLoaderVersion: (p: { gameVersion: string }) => `Keine Fabric-Loader-Version für Minecraft ${p.gameVersion} verfügbar.`,
      fabricProfileFetchFailed: (p: { gameVersion: string; loaderVersion: string; status: number | string }) =>
        `Fabric-Profil für ${p.gameVersion}/${p.loaderVersion} konnte nicht geladen werden: ${p.status}`,
      versionManifestFetchFailed: (p: { status: number | string }) => `Versions-Manifest konnte nicht geladen werden: ${p.status}`,
      versionNotFound: (p: { versionId: string }) => `Minecraft-Version ${p.versionId} nicht im Versions-Manifest gefunden.`,
      versionDetailFetchFailed: (p: { versionId: string; status: number | string }) =>
        `Versionsdetails für ${p.versionId} konnten nicht geladen werden: ${p.status}`,
      /** Fallback text only - `PlayScreen`'s Cancel button intercepts this code before it ever
       * reaches `formatError` (see `formatError.ts#errorCode`) and shows `play.launchCancelled`
       * instead, so this entry is only ever seen if some other, not-yet-updated call site ends up
       * showing a `launch.cancelled` error through the normal path. */
      cancelled: 'Start abgebrochen.',
      offlineNotReady:
        'Keine Internetverbindung, und für diese Instanz fehlen noch Dateien. Offline starten geht erst, nachdem die Instanz einmal online gestartet wurde.'
    },
    mods: {
      searchFailed: (p: { status: number | string }) => `Modrinth-Suche fehlgeschlagen (${p.status})`,
      versionsLoadFailed: (p: { status: number | string }) => `Konnte Modrinth-Versionen nicht laden (${p.status})`,
      hashLookupFailed: (p: { status: number | string }) => `Modrinth-Hash-Lookup fehlgeschlagen (${p.status})`,
      noFabricBuild: (p: { gameVersion: string; dependencyTitle?: string }) =>
        p.dependencyTitle
          ? `Abhängigkeit "${p.dependencyTitle}": Kein passender Fabric-Build für Minecraft ${p.gameVersion} gefunden.`
          : `Kein passender Fabric-Build für Minecraft ${p.gameVersion} gefunden.`,
      incompatibleWithInstalled: (p: { mod: string; installedMod: string }) =>
        `"${p.mod}" ist mit der bereits installierten Mod "${p.installedMod}" nicht kompatibel und kann deshalb nicht installiert werden.`,
      incompatibleWithEachOther: (p: { modA: string; modB: string }) =>
        `"${p.modA}" ist mit "${p.modB}" nicht kompatibel - beide wären Teil dieser Installation, das geht nicht.`,
      noDownloadableFile: 'Eine benötigte Modrinth-Version hat keine herunterladbare Datei.',
      modrinthVersionLoadFailed: (p: { versionId: string; status: number | string }) =>
        `Modrinth-Version ${p.versionId} konnte nicht geladen werden (${p.status}).`,
      modrinthVersionNoFile: (p: { versionId: string }) => `Modrinth-Version ${p.versionId} hat keine herunterladbare Datei.`
    },
    storage: {
      targetInsideSource: 'Der neue Speicherort darf nicht innerhalb des aktuellen Speicherorts liegen.'
    }
  }
}
