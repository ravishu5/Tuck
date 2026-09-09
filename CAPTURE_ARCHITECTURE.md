# Tuck — Capture Architecture

How Tuck turns a shared URL into a rich, searchable, offline archive entry.

This document exists because the current enrichment path — a single static HTTP GET parsed
with Jsoup — cannot see most of what modern platforms render, and because the fix is not one
patch but a layered strategy with very different cost and risk at each layer.

Research input: 474 userscripts surveyed on Greasy Fork (2026-09-02), full source read for
five; plus `yt-dlp` read directly on GitHub. Where a technique below came from a specific
source it is cited. Both are the right prior art because a Tampermonkey script, a yt-dlp
extractor and Tuck's enrichment worker are solving the identical problem — pull structured data
out of a page that does not want to give it up — and both have been maintained against live
site changes for years.

The two disagree usefully. Userscripts run *inside* a logged-in browser, so they show what is
reachable once you are already a real client; yt-dlp runs *outside* one, so it shows what a
server-side fetcher can still reach. Tuck is the second kind of client, which is why §4.4 ends
where it does.

---

## 1. Scope and boundary

Tuck captures **what the user can already see**. Two things get conflated under "userscript
power" and only one of them is in scope:

| In scope | Out of scope |
|---|---|
| Running as the logged-in user, capturing content they are entitled to | Circumventing entitlement — paywall bypass, ad-wall stripping |
| Public endpoints the platform itself serves to embedders | Credential-less access to paid content |
| Dismissing consent/interstitial/"open in app" nags | Anything that would make Tuck a piracy tool |

The line is not just legal. Google Play's Device and Network Abuse policy is the practical
constraint, and it is also, conveniently, the line that keeps the architecture simple: every
technique below either uses a documented-by-behaviour public endpoint or the user's own
session.

---

## 2. Why the current pipeline is insufficient

Six capabilities separate a userscript from `Jsoup.connect(url).get()`:

| Capability | Tuck today | Consequence |
|---|---|---|
| Post-JavaScript DOM | ❌ | Instagram, X, LinkedIn, Threads render client-side; only OG tags survive |
| The user's session cookies | ❌ | Root cause of the Reddit `.json` 403 in `FUTURE_WORK.md` |
| The page's own embedded JSON | ⚠️ one ad-hoc regex for Instagram `display_url` | Largest untapped source of structured data |
| Response interception | ❌ | Cannot see the private API the page calls itself |
| Cross-origin fetch | ✅ already have it | Non-issue: Android has no CORS |
| Declarative `@match` registry | ❌ hardcoded `domain.contains(...)` chains | Adding a site is a code change, not a data change |

The genuinely reusable architectural idea from Tampermonkey is the last row: **extraction
logic is data, versioned per site, matched by URL pattern.**

### Known defects this addresses

1. ~~**Every URL is fetched twice through two competing stacks.**~~ Fixed in 1a. The worker now
   makes one fetch through `SourceExtractorRegistry`; `UrlMetadataProcessor` is reduced to
   `cleanUrl` and `extractDomain`, and its Reddit/Instagram/YouTube/TikTok/LinkedIn handlers are
   gone — replaced by `InstagramSourceExtractor`, `TikTokSourceExtractor` and
   `LinkedInSourceExtractor`, which parse a payload someone else fetched and are therefore
   testable. The three duplicated `domain.contains("reddit")` platform guesses in the worker went
   with them: the registry already knows the platform.

2. ~~**`TwitterSourceExtractor` extracts nothing.**~~ Fixed in 1b. See §4.1.

3. **Nothing reads `ld+json`.** `grep` for `ld+json`, `__NEXT_DATA__`, `ytInitialPlayerResponse`
   across `app/src` returns zero hits.

4. **Article body extraction is naive.** `GenericWebSourceExtractor.kt:40` selects
   `article p, main p, div[itemprop=articleBody] p, p` and takes the first ten. On most sites
   that captures nav, cookie banner and footer text.

5. ~~**`og:image` URLs are fetched with no host allowlist.**~~ Fixed: `downloadAndCacheImage`
   now runs every hop through `RemoteMediaPolicy`. See §6.

---

## 3. The capture ladder

Four tiers, ordered by cost. **Climb only when the tier below returns thin.** The key finding
of the userscript survey is that most of the value sits in Tiers 0–1, which need no browser,
no JavaScript engine, and no new dependency — they stay pure-Kotlin and offline-unit-testable
against fixtures, which is what the `SourceExtractor` interface was designed for.

### Tier 0 — URL rewriting (no network, ~0 ms)

A large fraction of userscripts are only this. Pure functions, trivially testable alongside
`UrlNormalizationTest`.

| Rewrite | Why |
|---|---|
| `reddit.com/r/…` → `old.reddit.com/r/…` | Server-rendered HTML; no JS; friendlier than the `.json` endpoint that 403s |
| `youtu.be/ID` → `youtube.com/watch?v=ID` | Normalise before oEmbed |
| `arxiv.org/abs/X` → also read `/pdf/X` | Highwire `citation_*` meta tags |
| AMP → `link[rel=canonical]` | Often the only clean URL |
| `m.` / `mobile.` → desktop host | Desktop markup is usually richer |

Extend the tracking-parameter strip in `UrlMetadataProcessor.trackingParameters` with
`share_id`, `rdt`, `context`, `spm`, `__twitter_impression`.

### Tier 1 — Public endpoints and embedded JSON (one fetch, no JS engine)

Highest value-to-effort ratio in the whole document. Two sub-techniques.

**(a) Platform syndication/embed endpoints.** These are public, unauthenticated, and served by
the platform specifically so third parties can embed content. See §4 for the three that matter.

**(b) Embedded JSON mining.** Every modern site ships its structured data inside the HTML
already being downloaded.

`<script type="application/ld+json">` — schema.org — yields, deterministically and with no AI:

| Type | Fields | Tuck feature unlocked |
|---|---|---|
| `NewsArticle` / `BlogPosting` | headline, author, datePublished, articleBody | Real author → `PERSON` entity pipeline; real body → FTS |
| `VideoObject` | name, duration, uploadDate | Duration on cards, true publish date |
| `Recipe` | `recipeIngredient[]`, `recipeInstructions[]`, cookTime | Ingredients straight into `checklist_items` (schema v8) |
| `Product` | name, price, currency, availability | Price at save time; feeds `MONEY` entity + Shopping category |
| `Event` | startDate, location | Feeds the existing `ReminderScheduler` |
| `BreadcrumbList` | category path | Better classification signal than keyword rules |

Framework state blobs: `__NEXT_DATA__`, `window.__NUXT__`, `__APOLLO_STATE__`,
`window.__INITIAL_STATE__`. Generalise the existing Instagram `display_url` regex into a
reusable `JsonBlobMiner` applied to every domain.

### Tier 2 — Offscreen WebView execution — **shipped**

`CaptureEngine` renders a page in a headless WebView and hands the resulting HTML to the same
extractor that would have parsed a plain fetch. The extractors did not change: they receive
markup and parse it, and none of them knows whether it arrived from a socket or a rendered DOM.

```
ItemProcessingWorker
  ├─ Tier 0/1: SourceContentFetcher → extractor.extract(url, payload)
  └─ if extractor.requiresRenderedHtml && deepCaptureEnabled && result.isThin()
        └─ CaptureEngine.capture(url, readySelector) → extractor.extract(url, rendered)
              └─ adopted only when the render actually beat the cheap path
```

**Escalation is decided by the result, not the platform.** `isThin()` asks whether the parser
found anything a fetch had to supply — body, media or comments — as opposed to what the URL
already encoded. That means the expensive tier is skipped whenever the cheap one happened to
work, and is still attempted when a platform's markup changes under us without anyone editing a
flag. A render that comes back no better than the fetch is discarded rather than adopted.

**Waiting.** `onPageFinished` fires when the document loads, which on a single-page app is before
the content exists — precisely why Tier 1 fails on these pages. Extractors therefore declare a
`readySelector` meaning "the content has arrived", and the engine polls for it and returns as
soon as it matches. Without one it falls back to a fixed settle delay, which is slower and
guesses. Instagram's selector is `.Caption, .EmbeddedMediaImage, .CaptionUsername` — the classes
its parser actually reads, verified against the rendered page.

**Session sharing** is the point of the tier: the engine shares `CookieManager` with the
in-place viewer, so a platform the reader signed into once is captured as them from then on.

**Hardening**, per §6 and not optional given the above:

- No `addJavascriptInterface`. Content leaves through the `evaluateJavascript` return value only.
- `allowFileAccess`, `allowContentAccess`, and both `*FileURLs` flags off. A WebView holding the
  reader's session cookies and rendering pages Tuck did not write has no business reaching the
  filesystem.
- Requests to `AdHosts` are refused, shared with the viewer. A page rendered invisibly has even
  less business loading trackers than one the reader is looking at.
- Images are not loaded at all (`blockNetworkImage`), since nothing here is ever displayed. This
  is most of why a capture is cheaper than it looks.
- Output capped at 4 M characters.

**Costs**, which is why it is gated behind an off-by-default **Deep Capture** setting:

- 50–100 MB while a capture is in flight. Captures are serialised by a `Mutex` — two WebViews
  rendering at once on a mid-range phone is how a background worker gets killed.
- A few seconds per page against ~300 ms for Jsoup, bounded by a 20 s timeout.
- Real battery. It also loads pages using the reader's signed-in sessions, which the setting's
  subtitle says plainly rather than burying.

**What is not covered by tests.** The engine drives a real `WebView`, so it needs an instrumented
test rather than a JVM one; `CaptureEscalationTest` covers everything that decides *whether* it
runs, which is where a mistake is expensive rather than merely slow.

### Tier 3 — Response interception

`WebViewClient.shouldInterceptRequest` observes every XHR the page fires, letting Tuck keep the
JSON the site's own private API returned. Most powerful, most fragile, most likely to break on a
site deploy. Reserve for the few domains where nothing else works. See §6 for the mandatory
validation gate.

---

## 4. Verified platform recipes

Each of these was read from live, maintained userscript source. None requires a WebView.

### 4.1 X / Twitter — syndication endpoint

Source: `SourceCapsule` (Greasy Fork 584577, MIT).

X serves a public, unauthenticated endpoint returning a post's real text, author and media by
id. The token is a deterministic function of the id — no key, no cookie, no bearer:

```
https://cdn.syndication.twimg.com/tweet-result?id=<statusId>&lang=en&token=<token>

token = ((id / 1e15) * PI).toString(36) with all "0" and "." removed
```

The script's own note: this *"returns a tweet's real text, author, and media by id… sidesteps
the whole DOM problem."*

**Port caveat:** `id / 1e15` runs through a JavaScript double. Port with `Double`, not
`BigDecimal`, or the token differs and the endpoint 404s.

Response shape actually consumed:

| Path | Use |
|---|---|
| `text` | Body text |
| `user.name`, `user.screen_name` | Display name, `@handle` → `PERSON` entity |
| `created_at` | ISO-8601 timestamp |
| `entities.urls[].expanded_url` | Real destination behind each `t.co` |
| `entities.media[].url` | `t.co` links to strip from body text |
| `mediaDetails[]` / `photos[]` | `media_url_https`, `type` |
| `mediaDetails[].video_info.variants[]` | Filter `content_type == video/mp4`, sort by `bitrate` desc |
| `quoted_tweet` | Same shape, recursive |
| `note_tweet` | **Presence means the body is truncated** — long-form text is not served here |

The `note_tweet` flag is a definitive truncation signal, not a guess. Record it rather than
silently archiving a preview as if it were the full post.

The authenticated alternative — `x.com/i/api/graphql/{queryId}/TweetDetail` and
`TweetResultByRestId`, used by `Twitter Click'n'Save` (430132) — needs a bearer token and CSRF
header, i.e. a real session. Tier 2 only, and only if syndication proves insufficient.

### 4.2 Reddit — old.reddit.com HTML, now behind a session

Source: `Reddit - Thread with Comments to Markdown Exporter` (Greasy Fork 572667, MIT).

The exporter's strategy is to redirect to `old.reddit.com` and parse server-rendered HTML.
Selectors map directly to Jsoup:

| Field | Selector |
|---|---|
| Comment nodes | `:scope > .thing.comment` |
| Entry wrapper | `.entry` |
| Author | `.entry .author` |
| Body | `.entry .usertext-body .md` |
| Timestamp | `time[datetime]` |
| Score | `.score.unvoted` → `title` attribute |
| **Nesting** | `:scope > .child > .sitetable` (recurse) |

That recursion maps one-to-one onto the materialized-path scheme in `source_comments`
(2026-08-24 ADR), and `OldRedditCaptureTest` pins it against a fixture.

**Measured 2026-09-02: old.reddit no longer serves logged-out clients.** Every request — a real
post, a subreddit listing, even `.json` — answers `302 → /login/?reason=lor2`, with both desktop
and mobile user agents. `www.reddit.com` still returns 200, but only an 8 KB shell containing no
post, no comments and none of the modern element names. The public `.json` endpoint remains 403,
as `FUTURE_WORK.md` recorded.

So the earlier claim in this section — that old.reddit needs "no JavaScript, no login, no API
credentials" — held when it was written and does not hold now. **The markup did not change; the
access rule did.** The parser above is still exactly right for the page it targets.

That makes Reddit a session problem rather than a parsing one, which is precisely what Tier 2
exists for. `RedditSourceExtractor` now declares `requiresRenderedHtml`, so with Deep Capture on
and a reader signed into Reddit, the capture engine renders old.reddit with their cookies and
every selector above works unchanged. Logged out it degrades to the same fallback as before
rather than getting worse.

**In the viewer**, routing to old.reddit put a login page where the post used to be. It now loads
`www.reddit.com` with a **desktop user agent**, which is what actually removes the "open in the
app" interstitial — that screen is served to phone browsers and to nothing else. Chasing it with
a stylesheet would mean guessing at renamed custom elements; changing what the site thinks it is
talking to does not.

### 4.3 Instagram — public embed page only

**Status: the API path was investigated and rejected; the embed path shipped.**

`Instagram Download Button` (Greasy Fork 406535, 168k installs) scrapes `X-IG-App-ID` and a
media id out of the post page, then calls `api/v1/media/{mediaId}/info/`. `yt-dlp` reaches the
same endpoint far more cleanly — the app id is a constant (`936619743392459`) and the media id is
pure arithmetic on the shortcode, base-64 decoded against `A–Za–z0–9-_`, so no page fetch is
needed at all:

```python
def _id_to_pk(shortcode):
    if len(shortcode) > 28:
        shortcode = shortcode[:-28]
    return decode_base_n(shortcode, table=_ENCODING_CHARS)
```

**But that call sits inside `if self._is_logged_in:`.** The userscript works because it runs in a
browser with a session and sends `credentials: 'include'`. Tuck has neither. yt-dlp's logged-out
path instead posts to `instagram.com/api/graphql` with a CSRF token, an LSD token scraped from
the page, an `X-FB-Friendly-Name` operation name and a rotating `doc_id` — and it is guarded by
`if self._can_impersonate else None`, so without curl-impersonate TLS fingerprinting it is
skipped outright. The webpage fallback carries its own warning about being redirected to login
after anonymous rate limits.

Same three blockers as §4.4: TLS impersonation, rotating opaque tokens, aggressive anonymous
rate limiting.

**What shipped, and its measured limit.** `InstagramSourceExtractor` fetches
`/p/{shortcode}/embed/captioned/` — the surface Instagram serves to third-party embedders,
needing no session, app id or token. It replaces the four chained strategies in
`UrlMetadataProcessor.fetchInstagramMetadata`, none of which was testable.

**But that page is now client-rendered.** Fetched with curl on 2026-09-02, a live public reel
embed returned 611 KB containing no caption, no author, no post media URL and none of the markup
classes the parser selects on — only JavaScript bundles. Loaded in a real browser, every one of
those appears. So over plain HTTP the extractor yields only what the URL itself encodes: content
type, shortcode, canonical link, and the author of a story. The claim that this path captures
captions and posters, made when the section was first written, does not survive contact with the
live page.

The selectors are not wrong — they match the rendered DOM exactly. They are simply pointed at a
response that no longer contains anything. That makes this parser correct-but-starved until it is
handed rendered HTML, which is precisely what the Tier 2 engine in §3 exists to produce. Instagram
is therefore the strongest argument in this document for building Tier 2: there is no Tier 0 or
Tier 1 route to its content left.

**Reels specifically cannot be played inline at all.** Inspected in a browser, the logged-out
embed contains zero `<video>` elements — only a cover frame (`video_default_cover_frame` in the
CDN encode tag), a Play affordance and a "Watch on Instagram" link. The Play button is a
navigation, not a player. No selector or stylesheet work changes this; Instagram does not ship
the video to logged-out embedders. The viewer now intercepts that navigation and hands it to the
installed Instagram app rather than following it into a login wall.

One detail worth preserving from the userscript: Instagram's own chrome — sprite sheets, glyphs,
the app icon — is served from the same CDNs as post media, so a host check alone will happily
store the logo as the item's thumbnail. `isPostMedia` rejects it.

### 4.4 YouTube transcripts — spike done, recommended against

**Status: investigated against `yt-dlp` (2026-09-02) and dropped from the plan.** The earlier
hypothesis — one server-side GET of `captionTracks[].baseUrl` — is structurally correct and
practically unusable. `yt_dlp/extractor/youtube/_video.py` still reads exactly that path:

```python
for caption_track in traverse_obj(pctr, ('captionTracks', lambda _, v: v['baseUrl'])):
    base_url = caption_track['baseUrl']
```

Appending `&fmt=json3` returns clean JSON. Everything around that line is the problem.

**1. Proof-of-origin tokens.** Caption URLs are moving behind a PO token, generated by
YouTube's BotGuard attestation JavaScript:

```python
requires_pot = (
    any(e in traverse_obj(qs, ('exp', ...)) for e in ('xpe', 'xpv'))
    or (pot_policy.required and not (pot_policy.not_required_for_premium and is_premium_subscriber)))
```

The policy today is `SubsPoTokenPolicy(required=False)`, but the comments beside it read
`# In rollout, currently detected via experiment` and `# Premium users DO require a PO Token for
subtitles`. So it already fails for Premium accounts and for anyone YouTube has placed in the
`xpe`/`xpv` experiment. When no token is available yt-dlp does not degrade — it skips subtitles
for that client entirely. Generating a token means executing YouTube's attestation JS, which
needs a JS runtime, which is Tier 2.

**2. TLS impersonation.** Every subtitle URL yt-dlp emits carries `'impersonate': True` — it
fetches these through curl-impersonate to match a real browser's TLS fingerprint. Android's HTTP
stack cannot forge a JA3 fingerprint, so a plain `HttpURLConnection` is exactly the client this
check exists to reject.

**3. The player response is no longer in the page.** yt-dlp gets it from InnerTube POSTs with a
client context; `ytInitialPlayerResponse` survives in `_video.py` only inside a comment. The
single-GET premise was already out of date.

**Verdict.** Three independent, actively-maintained defences, any one of which turns transcript
capture into a feature that works for some users and silently fails for others. The maintenance
signal says the same thing: `_video.py` alone is 217 KB, there is a whole `pot` subpackage, and
the YouTube extractor took **20 commits in the last 90 days**. Tuck cannot carry that.

**What to do instead.** Two things, in order:

- **Now, Tier 1:** capture the video *description* and *chapters*, which are in the watch page
  and behind none of this. Chapters map onto `checklist_items`; the description is real text for
  FTS and usually carries the links and timestamps a viewer would want back. Most of the
  retrieval value at none of the cost.
- **Later, only if Tier 2 is built anyway:** a real `WebView` *is* a real browser — genuine TLS
  fingerprint, and YouTube's own JS can mint the PO token. The transcript then falls out of the
  approach the userscript in §9 already uses. Treat it as a Tier-2 follow-on, never as a
  justification for building Tier 2.

This also corrects an earlier claim in this document's history: transcripts were described as
costing nothing but a fetch. The APK-size half of that was right; the "just one GET" half was
not.

---

### 4.5 Google Maps — everything is in the URL

**Symptom:** a saved location had no title and no preview.

**Cause, measured 2026-09-02:** a Maps place page answers a fetch with 810 KB whose `<title>` is
empty and whose `og:title` and `og:description` are empty strings. The only `og:image` is the
generic Maps app icon. There is nothing on the page for the generic extractor to find, so it
produced an item with nothing on it.

**Fix:** stop reading the page. Maps encodes the place name and the coordinates in the URL path,
so `GoogleMapsSourceExtractor` needs no network at all for a full-form link:

| Form | Source |
|---|---|
| `/maps/place/Eiffel+Tower/@48.8584,2.2945,17z/` | name, viewport centre, zoom |
| `!3d48.8584!4d2.2945` in `data=` | the pin — preferred, since a share URL can be centred anywhere |
| `?q=48.8584,2.2945` | plain query form |
| `maps.app.goo.gl/…` | encodes nothing; see below |

Short links carry no payload, but Google leaves the resolved place path in the page it redirects
to as `window.ES5DGURL`, so following the redirect and reading that recovers the same
information. Both the place name and the raw coordinates go into `bodyText`, so an item is
findable by either.

**In the viewer**, a Maps place URL renders as the full Maps app — a consent prompt and an
"open in the app" nag over a map the reader cannot use. It now embeds the location instead, via
the keyless `output=embed` endpoint:

```
https://maps.google.com/maps?q=<lat,lng or name>&z=<zoom>&output=embed
```

The Maps Embed API proper requires an API key, which a local-first app with no server has nowhere
to put. `output=embed` is the long-standing keyless form and answers 200 with no consent
interstitial — verified against a live request. When neither coordinates nor a name are
recoverable the viewer falls back rather than embedding a map of nowhere.

**No thumbnail.** A static map image needs an API key, so a saved location has a title and a live
embed but no preview picture on the card. Nothing keyless exists that would not mean adding a
third-party tile provider.

---

## 5. Recipes as data

Steal the metadata block. A `CaptureRecipe` carries `@match` globs, a tier, a version, and
either a set of selectors (Tier 1) or a JS body (Tier 2). Adding a site stops being a code
change.

**Where recipes live** is a real product decision:

| Option | Verdict |
|---|---|
| Bundled in the APK (`assets/recipes/*.json`), updated by app update | **Recommended.** Compliant, CI-testable against golden fixtures |
| Remote-fetched recipe store | **Avoid.** Meets Play's Device and Network Abuse policy on downloading behaviour-changing executable code, and is an RCE surface pointed at the user's session cookies |
| User-pasted local recipes in Settings | Defensible middle ground — user-authored, not distributed by Tuck. Ship last, if at all |

---

## 6. Security requirements

Non-negotiable for anything above Tier 1.

**No `addJavascriptInterface`.** Use the `evaluateJavascript` return value only. Recipes return
JSON and nothing else. A WebView holding the user's session cookies plus an arbitrary-JS bridge
is remote code execution against their logged-in accounts.

**Lock the capture WebView down:** `allowFileAccess = false`, `allowContentAccess = false`,
cap the returned string length.

**Address policy before fetching any media byte.** *Shipped as `RemoteMediaPolicy`.*
`SourceCapsule` checks the host is `twimg.com` or a subdomain before every fetch, with the
comment that this *"bounds SSRF so a crafted media URL in a post cannot make the script fetch an
arbitrary origin."*

Tuck's version is address-based rather than a pure host allowlist, because Tuck saves the whole
web and legitimate thumbnails live on arbitrary CDNs. `downloadAndCacheImage` now:

- rejects any scheme but http/https, and any URL carrying userinfo (`http://cdn.example@10.0.0.1/`)
- resolves the host and blocks loopback, link-local, private, unique-local, carrier-grade-NAT,
  multicast and wildcard addresses — one private record poisons the whole name, since a mixed
  answer is a rebinding attempt rather than a fallback
- follows redirects **by hand**, re-checking every hop: an allowlist that trusts a `Location`
  header is not an allowlist
- requires an `image/*` content type and caps the body at 10 MB while copying, because a
  `Content-Length` is a claim rather than a guarantee

`RemoteMediaPolicy.isAllowedHost` keeps the tighter per-platform allowlist (`twimg.com`,
`cdninstagram.com`, `fbcdn.net`), matched on label boundaries so `twimg.com.evil.test` fails,
for media URLs read out of a platform payload where the host family is known ahead of time.

One residual gap, accepted deliberately: the JVM re-resolves the host when the socket opens, so
a TTL-0 record can in principle answer differently the second time. Closing that needs
connecting to the pinned address with a manual `Host` header, which breaks TLS verification
unless done very carefully — not worth the trade for a thumbnail.

**Validation gate on any intercepted response** — the pattern `SourceCapsule` uses for its
passive GraphQL capture layer, and the difference between an interception layer and a memory
leak with a security hole:

- Bound body length; bound URL length (≤ 4096)
- Reject payloads whose source is not the page itself, and whose origin differs
- FNV-1a hash signature for dedupe
- LRU-cap the signature set (they use 200)

---

## 7. A product signal from install counts

Sorted by total installs, the 474 surveyed scripts are not distributed the way the archive
thesis would predict:

| Script | Installs |
|---|---|
| Pagetual (infinite scroll) | 462,901 |
| Picviewer CE+ | 308,475 |
| Telegram Media Downloader | 233,459 |
| Image Downloader | 205,622 |
| Instagram Download Button | 168,236 |
| … | |
| Reddit Thread → Markdown Exporter | 26 |
| SourceCapsule (X threads → Markdown, offline HTML) | 19 |

`SourceCapsule` is almost exactly Tuck's pitch, expressed as a userscript, and has 19 installs
against a media downloader's 168,000. Two to four orders of magnitude.

The browser-extension market is not the mobile share-sheet market, and Tuck's value is
retrieval rather than acquisition — so this is not an argument to abandon the thesis. But it is
evidence that **downloading and keeping the actual media locally** deserves to rank above
transcript and reader extraction on the roadmap, and that "save the video and images, not just
a thumbnail" is the change a user would notice first.

---

## 8. Phased plan

Phase 1 is entirely pure Kotlin against the existing `SourceExtractor` interface, offline
unit-testable against checked-in fixtures, with no new dependency.

| Phase | Work | Unblocks |
|---|---|---|
| ~~1a~~ | ~~Collapse the two extraction stacks~~ — **shipped** | One fetch per link, one maintenance surface |
| ~~1b~~ | ~~X syndication endpoint (§4.1)~~ — **shipped**, `TwitterSyndicationTest` | Dead extractor → full text, author, media |
| ~~1c~~ | ~~`old.reddit.com` rewrite + Jsoup comment tree (§4.2)~~ — parser shipped; **access now needs Tier 2 + a session** | Comment trees, still no OAuth |
| ~~1d~~ | ~~Instagram app-id + media info API~~ → **shipped as the embed path** (§4.3); the API needs a session | Real captions, author, poster; feeds `media_assets` |
| 1e | `ld+json` + `JsonBlobMiner` as shared Tier-1 steps | Recipes, products, articles, events |
| 1f | Readability for article bodies (`readability4j`) | Backlog H3 (Reader view) |
| ~~—~~ | ~~Media host allowlist (§6)~~ — **shipped** as `RemoteMediaPolicy`, `RemoteMediaPolicyTest` | Closes an open SSRF path |
| — | ~~YouTube transcript~~ — **dropped** (§4.4); capture description + chapters instead | Transcript needs Tier 2; description/chapters are free |
| ~~2~~ | ~~`CaptureEngine` offscreen WebView + session sharing~~ — **shipped**, `CaptureEscalationTest` | Instagram captions and media; LinkedIn, logged-in Reddit and X remain available to it |
| 3 | Recipes as bundled JSON with `@match` patterns | Adding a site stops being a code change |

---

## 9. Sources

| Script | ID | Licence | Technique taken |
|---|---|---|---|
| SourceCapsule | 584577 | MIT | X syndication endpoint + token; network capture validation gate; SSRF host allowlist |
| Reddit Thread → Markdown Exporter | 572667 | MIT | `old.reddit.com` redirect; comment tree selectors |
| Instagram Download Button | 406535 | — | `X-IG-App-ID` + media info API |
| YouTube Transcript Downloader | 567166 | MIT | InnerTube transcript segment shape (Tier 2) |
| Twitter Click'n'Save | 430132 | — | Authenticated GraphQL endpoints (reference only) |
| `yt-dlp` (`extractor/youtube/`) | GitHub | Unlicense | PO-token and TLS-impersonation findings that closed §4.4 |
| `yt-dlp` (`extractor/instagram.py`) | GitHub | Unlicense | Shortcode→pk arithmetic; proof the media API needs a session (§4.3) |

Also relevant as prior art, not surveyed here: **Zotero's translator collection** — 600+
per-site JavaScript extractors, versioned, open source. The mature equivalent of §5 for
academic and bibliographic capture. Worth reading before finalising the recipe format.
