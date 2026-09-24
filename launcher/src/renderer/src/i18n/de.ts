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
    loginButton: 'Mit Microsoft anmelden'
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
    designHeading: 'Wähle das Design im Spiel',
    designExplanation:
      'Der Startbildschirm und das Client-Mods-Menü können wie normales Minecraft aussehen oder im eigenen Client-Design in deinen Farben. Du kannst das jederzeit in den Einstellungen oder direkt im Spiel auf dem Startbildschirm ändern.'
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
    play: 'Play',
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
    importResultOptions: 'Einstellungen importiert',
    importResultNone: 'Keine options.txt/Mods im gewählten Ordner gefunden.',
    versionListError: (error: string) => `Versionsliste konnte nicht geladen werden: ${error}`,
    existingHeading: 'Vorhandene Instanzen',
    active: 'Aktiv',
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
      'Standardmäßig aus - hier gezielt aktivieren. Fabric API und Sodium/Lithium (Performance, für gute Leistung auch auf schwächeren Geräten) sowie Continuity/3D Skin Layers (haben ihr eigenes An/Aus im Mod-Menü ingame) laufen immer mit und tauchen deshalb nicht als eigene Schalter auf.',
    bundleIncompatible: (versionId: string) =>
      `Wirkt sich aktuell nicht aus - ${versionId} hat kein Mod-Bundle, startet ohnehin ohne gebündelte Mods.`,
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
    working: 'Wird ausgeführt…'
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
      showSnapshots: 'Snapshots bei der Versionsauswahl anzeigen'
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
        `Minecraft-API-Aufruf fehlgeschlagen (${p.status}): ${p.detail}`
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
      libraryEntryNotFound: 'Dieses Cape ist nicht mehr in deiner Sammlung.',
      statusLoadFailed: (p: { status: number | string }) => `Cape-Status konnte nicht geladen werden (${p.status}).`,
      uploadFailed: (p: { status: number | string; detail: string }) => `Cape-Upload fehlgeschlagen (${p.status}): ${p.detail}`,
      deleteFailed: (p: { status: number | string; detail: string }) => `Cape entfernen fehlgeschlagen (${p.status}): ${p.detail}`
    },
    skin: {
      wrongDimensions: (p: { width: number; height: number }) =>
        `Minecraft-Skins müssen 64x64 (oder das alte 64x32-Format) sein, diese Datei ist ${p.width}x${p.height}.`,
      needInstanceFirst: 'Erst eine Instanz starten (Play-Klick), um die Steve/Alex-Vorlage laden zu können.',
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
      notFound: (p: { instanceId: string }) => `Instanz ${p.instanceId} nicht gefunden.`
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
      cancelled: 'Start abgebrochen.'
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
