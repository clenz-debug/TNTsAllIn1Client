/** Channel names shared between main and preload/renderer, kept in one place so both sides
 * stay in sync (renaming a channel here is a compile error everywhere it's used). */
export const IpcChannel = {
  AuthRestore: 'auth:restore',
  AuthLogin: 'auth:login',
  AuthLoginMock: 'auth:login-mock',
  AuthProgress: 'auth:progress',
  AuthProfile: 'auth:profile',
  LaunchPlay: 'launch:play',
  LaunchProgress: 'launch:progress',
  VersionsList: 'versions:list',
  GameLog: 'game:log',
  ShellOpenExternal: 'shell:open-external',
  SettingsLoad: 'settings:load',
  SettingsSave: 'settings:save',
  ModsListBundled: 'mods:list-bundled',
  ModsListCustom: 'mods:list-custom',
  ModsAddCustom: 'mods:add-custom',
  ModsRemoveCustom: 'mods:remove-custom',
  UpdateCheck: 'update:check'
} as const
