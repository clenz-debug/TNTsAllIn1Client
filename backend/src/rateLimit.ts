/** Fixed-window counter per key (player UUID or client IP) - small closed user group, in-memory is enough. */
export class RateLimiter {
  private readonly hits = new Map<string, { count: number; windowStart: number }>()

  constructor(
    private readonly limit: number,
    private readonly windowMs: number
  ) {}

  /** Counts one attempt; false once the key has used up its budget for the current window. */
  take(key: string): boolean {
    const now = Date.now()
    const entry = this.hits.get(key)
    if (!entry || now - entry.windowStart >= this.windowMs) {
      this.hits.set(key, { count: 1, windowStart: now })
      return true
    }
    entry.count++
    return entry.count <= this.limit
  }

  prune(): void {
    const now = Date.now()
    for (const [key, entry] of this.hits) {
      if (now - entry.windowStart >= this.windowMs) this.hits.delete(key)
    }
  }
}
