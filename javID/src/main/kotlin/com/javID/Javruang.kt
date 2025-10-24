package com.JavID

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
//import com.lagradost.cloudstream3.utils.httpsify
//import com.lagradost.cloudstream3.utils.loadExtractor
//import java.net.URI


class Javruang : MainAPI() {
    override var mainUrl = "https://ruangjav.com"
    private var directUrl: String? = null
    override var name = "Javruang"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "jav-sub-indo-terbaru" to "Terbaru"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/${request.data}/page/$page").document
        val home =  document.select("article.box").map { it.toSearchResult() }
            .mapNotNull { it.toSearchResult() }
        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = false
            ),
            hasNext = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse {
        val title     = this.select("a.tip").attr("title")
        val href      = this.select("a.tip").attr("href")
        val posterUrl = this.select("a.tip > div.limit > img").attr("src")
        //.replace(Regex("(_resized)?\\.webp$"), ".jpg")
        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    private fun Element.toSearchingResult(): SearchResponse {
        val title = this.select("div.details a").text()
        val href = fixUrl(this.select("div.image a").attr("href"))
        val posterUrl = this.select("div.image img").attr("src")
        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..2) {
            val document = app.get("${mainUrl}/jav/page/$i?s=$query").document

            val results = document.select("article")
                .mapNotNull { it.toSearchingResult() }

            if (!searchResponse.containsAll(results)) {
                searchResponse.addAll(results)
            } else {
                break
            }

            if (results.isEmpty()) break
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title =
            document.selectFirst("meta[property=og:title]")?.attr("content")?.trim().toString()
        val poster =
            document.selectFirst("meta[property=og:image]")?.attr("content")?.trim().toString()
        val description =
            document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val recommendations =
            document.select("ul.videos.related >  li").map {
                val recomtitle = it.selectFirst("div.video > a")?.attr("title")?.trim().toString()
                val recomhref = it.selectFirst("div.video > a")?.attr("href").toString()
                val recomposterUrl = it.select("div.video > a > div > img").attr("src")
                val recomposter = "https://javdoe.sh$recomposterUrl"
                newAnimeSearchResponse(recomtitle, recomhref, TvType.NSFW) {
                    this.posterUrl = recomposter
                }
            }
        //println(poster)
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.recommendations = recommendations
        }
    }


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val videoId = document.selectFirst("meta[name=video-id]")?.attr("content")
        val directUrl = "https://embedplayer.xyz/embed/index.php?id=$videoId"
        try {
            val videoId = data // or extract from URL if needed
            for (serverId in 1..8) {
                val apiUrl = "https://embedplayer.xyz/embed/get_server_url.php?video_id=$videoId&server_id=$serverId"
                val response = app.get(apiUrl).text
                val json = parseJson<ResponseModel>(response)
                if (json.success && json.url.isNotBlank()) {
                    // Hand off to Cloudstream’s extractor system
                    loadExtractor(json.url, directUrl, subtitleCallback, callback)
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }


    data class ResponseModel(
        val success: Boolean = false,
        val url: String? = null,
    )
    
}
