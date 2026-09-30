import { app } from 'electron'

/**
 * Modrinth's API terms ask every client to send a uniquely identifying User-Agent (project name,
 * version and a contact) - requests with a generic one may be blocked. JellySquid's OK for
 * installing Sodium through this launcher depends on following those terms (see Projekt_Roadmap.md's
 * license section), so every request to Modrinth (API and CDN) goes out with this header.
 */
export function modrinthHeaders(): Record<string, string> {
  return { 'User-Agent': `clenz-debug/TNTsAllIn1Client/${app.getVersion()} (github.com/clenz-debug/TNTsAllIn1Client)` }
}
