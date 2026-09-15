/** Ambient merge into electron-vite's own `ImportMetaEnv` (declared in `electron-vite/node.d.ts`,
 * see `tsconfig.node.json`'s `types`) - these `MAIN_VITE_*` vars are loaded from `launcher/.env`
 * (gitignored, see `.env.example`) and statically inlined into `out/main/index.js` at build time
 * by electron-vite's default `envPrefix` for the main process. Kept in its own file next to the
 * one module that actually reads them ({@link ./b2Config}) rather than a project-wide env.d.ts,
 * since nothing else in `main/` needs to know these exist. */
interface ImportMetaEnv {
  readonly MAIN_VITE_B2_KEY_ID?: string
  readonly MAIN_VITE_B2_APPLICATION_KEY?: string
  readonly MAIN_VITE_B2_BUCKET?: string
  readonly MAIN_VITE_B2_ENDPOINT?: string
  readonly MAIN_VITE_B2_REGION?: string
  readonly MAIN_VITE_B2_PUBLIC_BASE_URL?: string
}
