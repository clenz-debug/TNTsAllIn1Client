interface Props {
  onClose: () => void
}

interface CreditEntry {
  name: string
  license: string
  url: string
}

/**
 * Mirrors mod/src/main/java/com/tntsallin1client/menu/CreditsScreen.java entry-for-entry - keep
 * both lists in sync when a bundled mod/resourcepack is added or removed (see that file's own
 * comment for why this isn't automated).
 */
const ENTRIES: CreditEntry[] = [
  { name: 'Fabric Loader', license: 'Apache-2.0', url: 'https://github.com/FabricMC/fabric-loader' },
  { name: 'Fabric API', license: 'Apache-2.0', url: 'https://github.com/FabricMC/fabric' },
  { name: 'Sodium', license: 'PolyForm Shield License 1.0.0', url: 'https://github.com/CaffeineMC/sodium' },
  { name: 'Lithium', license: 'LGPL-3.0-only', url: 'https://github.com/CaffeineMC/lithium-fabric' },
  { name: 'Continuity', license: 'LGPL-3.0-only', url: 'https://github.com/PepperCode1/Continuity' },
  { name: '3D Skin Layers', license: 'tr7zw Protective License', url: 'https://github.com/tr7zw/3d-Skin-Layers' },
  {
    name: 'Default Dark Mode (resource pack)',
    license: 'CC-BY-NC-SA-4.0',
    url: 'https://github.com/nebuIr/Default-Dark-Mode'
  },
  {
    name: 'Bushy Vegetation (resource pack)',
    license: 'BSD-3-Clause',
    url: 'https://modrinth.com/resourcepack/bushy-vegetation'
  },
  {
    name: '3D Bushy Bushie (resource pack)',
    license: 'Apache-2.0',
    url: 'https://modrinth.com/resourcepack/3d-bushy-bushie'
  },
  { name: 'Mushrooms Plus (resource pack)', license: 'MIT', url: 'https://modrinth.com/resourcepack/mushrooms-plus' },
  {
    name: 'Vanilla Spinning Stonecutter (3D) (resource pack)',
    license: 'MIT',
    url: 'https://modrinth.com/resourcepack/vanilla-spinning-stonecutter-3d'
  },
  {
    name: 'Vanilla Tweaks (resource pack selection)',
    license: 'vanillatweaks.net Terms',
    url: 'https://vanillatweaks.net/terms/'
  }
]

export function CreditsScreen({ onClose }: Props) {
  function openLink(url: string): void {
    void window.api.openExternal(url)
  }

  return (
    <div className="credits-screen">
      <header>
        <strong>Drittanbieter-Credits</strong>
        <button className="link-button" onClick={onClose}>
          Zurück
        </button>
      </header>
      <ul className="credits-list">
        {ENTRIES.map((entry) => (
          <li key={entry.url}>
            <button className="credits-entry" onClick={() => openLink(entry.url)}>
              <span>{entry.name}</span>
              <span className="credits-license">{entry.license}</span>
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}
