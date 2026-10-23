# Automatic genre playlists

When a song goes from "not liked" to "liked", SimpMusic looks up its genres in the background and
files it into local playlists named `Genre · <Genre>` (e.g. `Genre · Hip-Hop`).

Liking never waits for the network: the lookup is queued and every failure stays in the background.

## Sources

Only two, both queried by exact artist + title (never by title alone):

| Source | Call | Notes |
| --- | --- | --- |
| Last.fm | `track.getTopTags`, `autocorrect=0` | Needs the API key (see below). Read-only, no login. |
| MusicBrainz | `recording` search (one request) | No key. Results are *community tags* (a curated `genres` list is used when present). A recording is accepted only if its score is >= 90 **and** its title **and** an artist credit match. |

Both return **tags**, not verified genres. They go through the same filtering before use.

## Normalisation and selection

`GenreNormalizer` / `GenreMerger` (in `core/domain/.../genre`):

1. Tags are lower-cased and trimmed; "x music" becomes "x". Variants map to one key
   (`hip-hop`, `Hip Hop`, `hiphop` -> `hip-hop`; `R&B`, `rnb` -> `r-and-b`). The key identifies the
   genre and its playlist; the label is only for display.
2. Dropped: empty or over-long tags, listener chatter (`seen live`, `favorites`), vocal-gender
   markers, decades, bare nationalities, and tags equal to the artist or the title.
3. A precise style is never folded into its parent (`indie rock` stays apart from `rock`).
4. Each source's weight is 0..100; the combined score is a noisy-or, so two sources agreeing rank
   a tag higher. A genre is kept if its score is >= 0.40, **or** both sources returned it, **or** it is
   the best one and scores >= 0.20. At most 5 per song.
5. Nothing is invented: every genre comes from a returned tag, with its source and weight stored.

## Playlists

* One playlist per genre key, found through the `genre_playlist` table, **not** by title. A manual
  playlist is never touched, reused, or deleted, even if it is called `Genre · Rock`.
* Only additions. Unliking a song, or a genre vanishing from a newer lookup, removes nothing.
* A song is added at most once per playlist; repeating the lookup gives the same state.
* If you delete an automatic playlist, the next song of that genre recreates it (with that song only).

## Resilience

* Each provider has its own state (`pending / ok / no result / temporary error / skipped`) in
  `genre_lookup`. "Answered, nothing found" is final; a network error, 5xx or rate limit is retried
  with exponential backoff (1 min doubling, capped at 6 h, +-25 % jitter), at most 6 attempts.
* A provider that answered is never asked again, so one failing provider doesn't redo the other.
  Whatever is known is filed after every attempt.
* Unfinished songs resume 20 s after startup. Songs that gave up are reopened after 24 h.
* Rate limits: MusicBrainz one request per 1.1 s; Last.fm one per 0.3 s.

## Configuration

| Setting | Where | Effect |
| --- | --- | --- |
| Settings → *Last.fm API key (genre playlists)* | In the app (Full build only) | Your own 32-hex Last.fm key (https://www.last.fm/api/account/create). Public tags need no secret and no login. Empty = Last.fm skipped, MusicBrainz only. |
| `LASTFM_API_KEY`, `LASTFM_SECRET` | `local.properties` (optional) | Build-time fallback key, also used by scrobbling. The typed key wins. |

The key is stored in the app's DataStore (`lastfm_tag_api_key`) and read at each lookup. The FOSS build has no Last.fm code and hides the field.

MusicBrainz is identified with the User-Agent `SimpMusic-GenrePlaylists/1.0 ( https://github.com/tlenenao/SimpMusic )`
(`MusicBrainzGenreProvider.USER_AGENT`); change the contact URL if you rename the fork.

## Database

Version 28 adds `song_genre_tag`, `genre_lookup`, `genre_playlist` (three new tables, auto-migration
27 -> 28 and 26/25 -> 28; no existing table is altered).

## Tests

```bash
./gradlew :domain:jvmTest :lastfm:jvmTest :data:jvmTest
```

No test calls a real API: providers take a fake HTTP function.
