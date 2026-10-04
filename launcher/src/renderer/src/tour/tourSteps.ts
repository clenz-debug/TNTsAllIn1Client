import type { de } from '../i18n/de'

/** Which of PlayScreen's screens a step needs open - PlayScreen switches to it when the step starts. */
export type TourScreen = 'play' | 'settings' | 'skin' | 'capes' | 'friends' | 'instances' | 'mods' | 'worlds' | 'resourcepacks'

export type TourStepId = keyof typeof de.tour.steps

export interface TourStep {
  id: TourStepId
  screen: TourScreen
  /** CSS selectors; the spotlight covers all matching elements together. */
  targets: string[]
  /** Clicks and typing inside the spotlight reach the page (only the "create an instance" step) -
   * everywhere else the whole launcher is blocked while the tour runs, logging out included. */
  interactive?: boolean
  /** "Weiter" stays disabled until this happened (the step still offers to skip it). */
  waitFor?: 'instanceCreated'
  /** Steps that make no sense in the current state are skipped: friends need an online profile
   * (and a loaded friends list to open the screen), mods/worlds/resource packs need an instance. The
   * mods screen's sections only exist for an instance with a mod loader; a legacy instance (before
   * 1.14) shows a note there instead, which gets a step of its own (own user report: the bundled
   * mods step stood over an empty screen). */
  needs?: 'online' | 'friends' | 'instance' | 'modLoaderInstance' | 'legacyInstance'
}

const tour = (name: string): string => `[data-tour="${name}"]`

/** Own user request, in exactly this order: account button (without being able to log out),
 * settings, credits (short), skin, capes, friends, managing instances (creating one), the instance
 * picker, then mods, worlds and resource packs - each button first on the play screen, then a short
 * look inside the screen it opens. Ends on the Play button. */
export const TOUR_STEPS: TourStep[] = [
  { id: 'account', screen: 'play', targets: ['.play-screen .player-menu-trigger'] },

  { id: 'settingsButton', screen: 'play', targets: [tour('header-settings')] },
  { id: 'settingsMemory', screen: 'settings', targets: [tour('settings-memory'), tour('settings-storage')] },
  { id: 'settingsGeneral', screen: 'settings', targets: [tour('settings-snapshots'), tour('settings-console')] },
  { id: 'settingsAppearance', screen: 'settings', targets: [tour('settings-appearance')] },
  { id: 'settingsLanguage', screen: 'settings', targets: [tour('settings-language'), tour('settings-design'), tour('settings-tour')] },

  { id: 'credits', screen: 'play', targets: [tour('header-credits')] },

  { id: 'skinButton', screen: 'play', targets: [tour('header-skin')] },
  { id: 'skinCurrent', screen: 'skin', targets: [tour('skin-current')] },
  { id: 'skinLibrary', screen: 'skin', targets: [tour('skin-library')] },
  { id: 'skinUpload', screen: 'skin', targets: [tour('skin-upload')] },

  { id: 'capesButton', screen: 'play', targets: [tour('header-capes')] },
  { id: 'capesCreate', screen: 'capes', targets: [tour('capes-main')] },
  { id: 'capesCollection', screen: 'capes', targets: [tour('capes-collection')] },

  { id: 'friendsButton', screen: 'play', targets: [tour('header-friends')], needs: 'online' },
  { id: 'friendsOwn', screen: 'friends', targets: [tour('friends-own')], needs: 'friends' },
  { id: 'friendsAdd', screen: 'friends', targets: [tour('friends-add')], needs: 'friends' },
  { id: 'friendsList', screen: 'friends', targets: [tour('friends-list')], needs: 'friends' },

  { id: 'instancesButton', screen: 'play', targets: [tour('manage-instances')] },
  { id: 'instancesCreate', screen: 'instances', targets: [tour('instances-create')], interactive: true, waitFor: 'instanceCreated' },
  { id: 'instancesList', screen: 'instances', targets: [tour('instances-list')] },
  { id: 'instancePicker', screen: 'play', targets: ['label[for="instance-select"]', '#instance-select'] },

  { id: 'modsButton', screen: 'play', targets: [tour('mods-button')] },
  { id: 'modsBundled', screen: 'mods', targets: [tour('mods-bundled')], needs: 'modLoaderInstance' },
  { id: 'modsSearch', screen: 'mods', targets: [tour('mods-search')], needs: 'modLoaderInstance' },
  { id: 'modsOwn', screen: 'mods', targets: [tour('mods-own')], needs: 'modLoaderInstance' },
  { id: 'modsLegacy', screen: 'mods', targets: [tour('mods-legacy')], needs: 'legacyInstance' },

  { id: 'worldsButton', screen: 'play', targets: [tour('worlds-button')] },
  { id: 'worlds', screen: 'worlds', targets: [tour('worlds-main')], needs: 'instance' },

  { id: 'resourcepacksButton', screen: 'play', targets: [tour('resourcepacks-button')] },
  { id: 'resourcepacks', screen: 'resourcepacks', targets: [tour('resourcepacks-main')], needs: 'instance' },

  { id: 'play', screen: 'play', targets: [tour('play-button')] }
]

export interface TourContext {
  online: boolean
  friendsReady: boolean
  hasInstance: boolean
  /** The selected instance is a legacy version (before 1.14, no mod loader). */
  legacyInstance: boolean
}

export function isTourStepAvailable(step: TourStep, context: TourContext): boolean {
  switch (step.needs) {
    case 'online':
      return context.online
    case 'friends':
      return context.online && context.friendsReady
    case 'instance':
      return context.hasInstance
    case 'modLoaderInstance':
      return context.hasInstance && !context.legacyInstance
    case 'legacyInstance':
      return context.hasInstance && context.legacyInstance
    default:
      return true
  }
}

/** The next (or previous) step that's available right now, skipping the ones that aren't. */
export function neighborTourStep(currentId: TourStepId, direction: 1 | -1, context: TourContext): TourStep | null {
  for (let index = TOUR_STEPS.findIndex((step) => step.id === currentId) + direction; index >= 0 && index < TOUR_STEPS.length; index += direction) {
    if (isTourStepAvailable(TOUR_STEPS[index], context)) return TOUR_STEPS[index]
  }
  return null
}
