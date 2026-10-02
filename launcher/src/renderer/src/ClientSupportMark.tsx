import { useTranslations } from './i18n/LanguageContext'
import { Logo } from './Logo'

/**
 * The client logo as a "the client supports this Minecraft version" marker (own user request) -
 * shown in front of instances and versions alike. Fills whatever slot it is put into; on a
 * dropdown's highlighted row it gets a `--bg-panel` backdrop (see `global.css`), since one of its
 * beams would vanish on that accent color otherwise.
 */
export function ClientSupportMark() {
  const t = useTranslations()
  return (
    <span className="client-support-mark" title={t.instances.clientSupported}>
      <Logo variant="mark" label={t.instances.clientSupported} />
    </span>
  )
}
