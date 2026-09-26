package com.stash.desktop.di

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Singleton

/**
 * Desktop-only OkHttp interceptor registered via Dagger multibinding (`@IntoSet Interceptor`).
 *
 * 1. Normalizes Spotify `sp_dc` cookies on `open.spotify.com/api/token` requests if the user pasted
 *    a full `sp_dc=...` header or quoted value.
 * 2. Synthesizes `"Albums"` and `"Artists"` `musicShelfRenderer` sections on unauthenticated
 *    `music.youtube.com/youtubei/v1/search` responses when YouTube Music returns album/artist
 *    `MPREb_` / `UC` browseEndpoints inside `musicCardShelfRenderer` or track `flexColumns`
 *    instead of a top-level titled `"Albums"` / `"Artists"` shelf, fixing `"View album"` and
 *    `"View artist"` navigation without modifying upstream code.
 */
@Module
object DesktopNetworkModule {

    @Provides
    @Singleton
    @IntoSet
    fun provideDesktopNetworkInterceptor(): Interceptor = Interceptor { chain ->
        var request = chain.request()
        val url = request.url

        if (url.host.equals("open.spotify.com", ignoreCase = true) && url.encodedPath == "/api/token") {
            val cookieHeader = request.header("Cookie")
            if (!cookieHeader.isNullOrBlank()) {
                val normalized = normalizeSpotifyCookieHeader(cookieHeader)
                if (normalized != cookieHeader) {
                    request = request.newBuilder()
                        .header("Cookie", normalized)
                        .build()
                }
            }
        }

        val response = chain.proceed(request)

        if (url.host.equals("music.youtube.com", ignoreCase = true) &&
            url.encodedPath.startsWith("/youtubei/v1/search") &&
            response.isSuccessful
        ) {
            val body = response.body ?: return@Interceptor response
            val contentType = body.contentType()
            val raw = body.string()
            val patched = runCatching { augmentYtMusicSearchResponse(raw) }.getOrDefault(raw)
            return@Interceptor response.newBuilder()
                .body(patched.toResponseBody(contentType ?: "application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()
        }

        response
    }

    private fun normalizeSpotifyCookieHeader(header: String): String {
        var v = header.trim()
        if (v.startsWith("sp_dc=", ignoreCase = true)) {
            v = v.substringAfter("=").trim()
        }
        if (v.startsWith("sp_dc=", ignoreCase = true)) {
            v = v.substringAfter("=").trim()
        }
        if (';' in v) {
            val spDcPart = v.split(';')
                .map { it.trim() }
                .firstOrNull { it.startsWith("sp_dc=", ignoreCase = true) }
            if (spDcPart != null) {
                v = spDcPart.substringAfter("=").trim()
            } else {
                v = v.substringBefore(";").trim()
            }
        }
        v = v.removeSurrounding("\"").removeSurrounding("'").trim()
        return "sp_dc=$v"
    }

    private fun augmentYtMusicSearchResponse(rawJson: String): String {
        val root = JSONObject(rawJson)
        val sectionListContents = root
            .optJSONObject("contents")
            ?.optJSONObject("tabbedSearchResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents")
            ?: return rawJson

        var hasAlbumsShelf = false
        var hasArtistsShelf = false
        for (i in 0 until sectionListContents.length()) {
            val shelf = sectionListContents.optJSONObject(i)?.optJSONObject("musicShelfRenderer") ?: continue
            val title = shelf.optJSONObject("title")
                ?.optJSONArray("runs")
                ?.optJSONObject(0)
                ?.optString("text", "")
                .orEmpty()
            if (title.equals("Albums", ignoreCase = true)) hasAlbumsShelf = true
            if (title.equals("Artists", ignoreCase = true)) hasArtistsShelf = true
        }
        if (hasAlbumsShelf && hasArtistsShelf) return rawJson

        data class FoundAlbum(val browseId: String, val title: String, val artist: String, val thumbUrl: String?)
        data class FoundArtist(val browseId: String, val name: String, val thumbUrl: String?)

        val albums = LinkedHashMap<String, FoundAlbum>()
        val artists = LinkedHashMap<String, FoundArtist>()

        fun extractThumb(node: JSONObject?): String? {
            val thumbs = node?.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
                ?: return null
            return if (thumbs.length() > 0) thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") else null
        }

        fun walk(node: Any?) {
            when (node) {
                is JSONObject -> {
                    val card = node.optJSONObject("musicCardShelfRenderer")
                    if (card != null) {
                        val nav = card.optJSONObject("title")
                            ?.optJSONArray("runs")
                            ?.optJSONObject(0)
                            ?.optJSONObject("navigationEndpoint")
                            ?: card.optJSONObject("onTap")
                        val browseEp = nav?.optJSONObject("browseEndpoint")
                        val browseId = browseEp?.optString("browseId", "").orEmpty()
                        val pageType = browseEp
                            ?.optJSONObject("browseEndpointContextSupportedConfigs")
                            ?.optJSONObject("browseEndpointContextMusicConfig")
                            ?.optString("pageType", "")
                            .orEmpty()
                        val titleText = card.optJSONObject("title")
                            ?.optJSONArray("runs")
                            ?.optJSONObject(0)
                            ?.optString("text", "")
                            .orEmpty()
                        val subtitleRuns = card.optJSONObject("subtitle")?.optJSONArray("runs")
                        var subtitleArtist = ""
                        if (subtitleRuns != null) {
                            for (r in 0 until subtitleRuns.length()) {
                                val runObj = subtitleRuns.optJSONObject(r) ?: continue
                                val txt = runObj.optString("text", "")
                                val runBrowse = runObj.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
                                val runBrowseId = runBrowse?.optString("browseId", "").orEmpty()
                                if (runBrowseId.startsWith("UC") || runBrowseId.startsWith("MPLAUC")) {
                                    if (subtitleArtist.isEmpty()) subtitleArtist = txt
                                    artists.putIfAbsent(runBrowseId, FoundArtist(runBrowseId, txt, null))
                                } else if (subtitleArtist.isEmpty() &&
                                    txt != " • " &&
                                    !txt.equals("Album", ignoreCase = true) &&
                                    !txt.equals("Single", ignoreCase = true) &&
                                    !txt.equals("EP", ignoreCase = true) &&
                                    !txt.equals("Song", ignoreCase = true)
                                ) {
                                    subtitleArtist = txt
                                }
                            }
                        }
                        val thumb = extractThumb(card)
                        if ((browseId.startsWith("MPREb_") || pageType == "MUSIC_PAGE_TYPE_ALBUM") && titleText.isNotEmpty()) {
                            albums.putIfAbsent(browseId, FoundAlbum(browseId, titleText, subtitleArtist, thumb))
                        } else if ((browseId.startsWith("UC") || browseId.startsWith("MPLAUC") || pageType == "MUSIC_PAGE_TYPE_ARTIST") && titleText.isNotEmpty()) {
                            artists.putIfAbsent(browseId, FoundArtist(browseId, titleText, thumb))
                        }
                    }

                    val item = node.optJSONObject("musicResponsiveListItemRenderer")
                    if (item != null) {
                        val thumb = extractThumb(item)
                        val itemBrowse = item.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
                        val itemBrowseId = itemBrowse?.optString("browseId", "").orEmpty()
                        val itemPageType = itemBrowse
                            ?.optJSONObject("browseEndpointContextSupportedConfigs")
                            ?.optJSONObject("browseEndpointContextMusicConfig")
                            ?.optString("pageType", "")
                            .orEmpty()

                        val flexCols = item.optJSONArray("flexColumns")
                        var firstColText = ""
                        var discoveredArtist = ""
                        if (flexCols != null) {
                            for (c in 0 until flexCols.length()) {
                                val runs = flexCols.optJSONObject(c)
                                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                    ?.optJSONObject("text")
                                    ?.optJSONArray("runs")
                                    ?: continue
                                for (r in 0 until runs.length()) {
                                    val runObj = runs.optJSONObject(r) ?: continue
                                    val txt = runObj.optString("text", "")
                                    if (c == 0 && r == 0) firstColText = txt
                                    val browseEp = runObj.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
                                    val bId = browseEp?.optString("browseId", "").orEmpty()
                                    val pType = browseEp
                                        ?.optJSONObject("browseEndpointContextSupportedConfigs")
                                        ?.optJSONObject("browseEndpointContextMusicConfig")
                                        ?.optString("pageType", "")
                                        .orEmpty()
                                    if ((bId.startsWith("UC") || bId.startsWith("MPLAUC") || pType == "MUSIC_PAGE_TYPE_ARTIST") && txt.isNotEmpty()) {
                                        if (discoveredArtist.isEmpty()) discoveredArtist = txt
                                        artists.putIfAbsent(bId, FoundArtist(bId, txt, thumb))
                                    } else if ((bId.startsWith("MPREb_") || pType == "MUSIC_PAGE_TYPE_ALBUM") && txt.isNotEmpty()) {
                                        albums.putIfAbsent(bId, FoundAlbum(bId, txt, discoveredArtist, thumb))
                                    }
                                }
                            }
                        }

                        if ((itemBrowseId.startsWith("MPREb_") || itemPageType == "MUSIC_PAGE_TYPE_ALBUM") && firstColText.isNotEmpty()) {
                            albums.putIfAbsent(itemBrowseId, FoundAlbum(itemBrowseId, firstColText, discoveredArtist, thumb))
                        } else if ((itemBrowseId.startsWith("UC") || itemBrowseId.startsWith("MPLAUC") || itemPageType == "MUSIC_PAGE_TYPE_ARTIST") && firstColText.isNotEmpty()) {
                            artists.putIfAbsent(itemBrowseId, FoundArtist(itemBrowseId, firstColText, thumb))
                        }
                    }

                    val keys = node.keys()
                    while (keys.hasNext()) {
                        walk(node.opt(keys.next()))
                    }
                }
                is JSONArray -> {
                    for (i in 0 until node.length()) {
                        walk(node.opt(i))
                    }
                }
            }
        }

        walk(sectionListContents)

        if (!hasAlbumsShelf && albums.isNotEmpty()) {
            val itemsArray = JSONArray()
            for (alb in albums.values) {
                val subtitleRuns = JSONArray().apply {
                    put(JSONObject().put("text", "Album"))
                    if (alb.artist.isNotEmpty()) {
                        put(JSONObject().put("text", " • "))
                        put(JSONObject().put("text", alb.artist))
                    }
                }
                val flexColumns = JSONArray()
                    .put(
                        JSONObject().put(
                            "musicResponsiveListItemFlexColumnRenderer",
                            JSONObject().put(
                                "text",
                                JSONObject().put("runs", JSONArray().put(JSONObject().put("text", alb.title))),
                            ),
                        ),
                    )
                    .put(
                        JSONObject().put(
                            "musicResponsiveListItemFlexColumnRenderer",
                            JSONObject().put("text", JSONObject().put("runs", subtitleRuns)),
                        ),
                    )
                val itemRenderer = JSONObject()
                    .put(
                        "navigationEndpoint",
                        JSONObject().put(
                            "browseEndpoint",
                            JSONObject()
                                .put("browseId", alb.browseId)
                                .put(
                                    "browseEndpointContextSupportedConfigs",
                                    JSONObject().put(
                                        "browseEndpointContextMusicConfig",
                                        JSONObject().put("pageType", "MUSIC_PAGE_TYPE_ALBUM"),
                                    ),
                                ),
                        ),
                    )
                    .put("flexColumns", flexColumns)
                if (!alb.thumbUrl.isNullOrBlank()) {
                    itemRenderer.put(
                        "thumbnail",
                        JSONObject().put(
                            "musicThumbnailRenderer",
                            JSONObject().put(
                                "thumbnail",
                                JSONObject().put(
                                    "thumbnails",
                                    JSONArray().put(JSONObject().put("url", alb.thumbUrl).put("width", 544).put("height", 544)),
                                ),
                            ),
                        ),
                    )
                }
                itemsArray.put(JSONObject().put("musicResponsiveListItemRenderer", itemRenderer))
            }
            val albumShelf = JSONObject().put(
                "musicShelfRenderer",
                JSONObject()
                    .put("title", JSONObject().put("runs", JSONArray().put(JSONObject().put("text", "Albums"))))
                    .put("contents", itemsArray),
            )
            sectionListContents.put(albumShelf)
        }

        if (!hasArtistsShelf && artists.isNotEmpty()) {
            val itemsArray = JSONArray()
            for (art in artists.values) {
                val flexColumns = JSONArray()
                    .put(
                        JSONObject().put(
                            "musicResponsiveListItemFlexColumnRenderer",
                            JSONObject().put(
                                "text",
                                JSONObject().put("runs", JSONArray().put(JSONObject().put("text", art.name))),
                            ),
                        ),
                    )
                    .put(
                        JSONObject().put(
                            "musicResponsiveListItemFlexColumnRenderer",
                            JSONObject().put(
                                "text",
                                JSONObject().put("runs", JSONArray().put(JSONObject().put("text", "Artist"))),
                            ),
                        ),
                    )
                val itemRenderer = JSONObject()
                    .put(
                        "navigationEndpoint",
                        JSONObject().put(
                            "browseEndpoint",
                            JSONObject()
                                .put("browseId", art.browseId)
                                .put(
                                    "browseEndpointContextSupportedConfigs",
                                    JSONObject().put(
                                        "browseEndpointContextMusicConfig",
                                        JSONObject().put("pageType", "MUSIC_PAGE_TYPE_ARTIST"),
                                    ),
                                ),
                        ),
                    )
                    .put("flexColumns", flexColumns)
                if (!art.thumbUrl.isNullOrBlank()) {
                    itemRenderer.put(
                        "thumbnail",
                        JSONObject().put(
                            "musicThumbnailRenderer",
                            JSONObject().put(
                                "thumbnail",
                                JSONObject().put(
                                    "thumbnails",
                                    JSONArray().put(JSONObject().put("url", art.thumbUrl).put("width", 544).put("height", 544)),
                                ),
                            ),
                        ),
                    )
                }
                itemsArray.put(JSONObject().put("musicResponsiveListItemRenderer", itemRenderer))
            }
            val artistShelf = JSONObject().put(
                "musicShelfRenderer",
                JSONObject()
                    .put("title", JSONObject().put("runs", JSONArray().put(JSONObject().put("text", "Artists"))))
                    .put("contents", itemsArray),
            )
            sectionListContents.put(artistShelf)
        }

        return root.toString()
    }
}
