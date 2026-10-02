import { mkdir, writeFile } from 'node:fs/promises'
import { join } from 'node:path'

/**
 * Every legacy version (`shared/legacyVersions.ts`) ships a log4j that resolves `${jndi:...}`
 * lookups inside logged text - any chat line can make the game load code from a server
 * ("Log4Shell", CVE-2021-44228). Mojang's fix is a replacement log4j configuration handed to the
 * game via `-Dlog4j.configurationFile` (the version JSON's `logging.client` entry); the one for
 * 1.7 - 1.11.2 (`client-1.7.xml`) drops every message containing `${...}`.
 *
 * This is that file with one change: Mojang's writes the console output as XML events (their
 * launcher parses those), ours keeps the plain pattern the game's own built-in configuration uses,
 * since the launcher's console shows the game's output as it comes. Used for 1.12.2 and 1.13.2 as
 * well, whose log4j (2.8.1) understands the same file.
 *
 * One filter of our own on top: 1.7.10 and 1.8.9 log the session's access token on startup
 * ("Session ID is token:..."). Mojang's launcher blanks it in its log view; here the line is
 * dropped, so the token ends up neither in the launcher's console nor in `logs/latest.log` - both
 * get copied into bug reports.
 */
const LEGACY_LOG_CONFIG = `<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <Console name="SysOut" target="SYSTEM_OUT">
            <PatternLayout pattern="[%d{HH:mm:ss}] [%t/%level]: %msg%n" />
        </Console>
        <RollingRandomAccessFile name="File" fileName="logs/latest.log" filePattern="logs/%d{yyyy-MM-dd}-%i.log.gz">
            <PatternLayout pattern="[%d{HH:mm:ss}] [%t/%level]: %msg%n" />
            <Policies>
                <TimeBasedTriggeringPolicy />
                <OnStartupTriggeringPolicy />
            </Policies>
        </RollingRandomAccessFile>
    </Appenders>
    <Loggers>
        <Root level="info">
            <filters>
                <MarkerFilter marker="NETWORK_PACKETS" onMatch="DENY" onMismatch="NEUTRAL" />
                <RegexFilter regex="(?s).*\\$\\{[^}]*\\}.*" onMatch="DENY" onMismatch="NEUTRAL"/>
                <RegexFilter regex="\\(Session ID is token:.*" onMatch="DENY" onMismatch="NEUTRAL"/>
            </filters>
            <AppenderRef ref="SysOut"/>
            <AppenderRef ref="File"/>
        </Root>
    </Loggers>
</Configuration>
`

/** Writes the configuration next to the instance's `game/` folder (rewritten on every launch, so a
 * launcher update that changes it always reaches existing instances) and returns its path. */
export async function writeLegacyLogConfig(instanceDir: string): Promise<string> {
  const path = join(instanceDir, 'log4j2-legacy.xml')
  await mkdir(instanceDir, { recursive: true })
  await writeFile(path, LEGACY_LOG_CONFIG, 'utf8')
  return path
}
