package com.javraja

import android.util.Base64
import com.lagradost.api.Log
//import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.httpsify
import com.lagradost.cloudstream3.utils.loadExtractor


class javraja : MainAPI() {
    override var mainUrl              = "https://ruangjav.com"
    override var name                 = "Javraja"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        // Fixed entries
//            "" to "Main Page",
            "jav-sub-indo-terbaru" to "Terbaru",
            //"category/english-subbed/?orderby=likes&order=DESC" to "English Subbed most likes",
            //"tag/for-women" to "For Women",
            //"actress/hanamiya-amu" to "Amu",
    )

    
 override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get("$mainUrl/${request.data}/page/?page=$page").document
        val home =  document.select("div.bx").map { it.toSearchResult() }
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
        val posterUrl = this.select("a.tip > div.limit > img").attr("src").replace(Regex("(_resized)?\\.webp$"), ".jpg")
        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..3) {
            val document = app.get("${mainUrl}/page/$i/?s=$query").document
            val results = document.select("#main > div").mapNotNull { it.toSearchResult() }
            if (!searchResponse.containsAll(results)) {
                searchResponse.addAll(results)
            } else {
                break
            }
            if (results.isEmpty()) break
        }
        return searchResponse
    }


    private suspend fun findposAct(namanya: String) : String {
        val encodedName = namanya.trim().replace(" ", "+")
        val document = app.get("${mainUrl}/actress-search/?taxonomy_search=$encodedName").document
        
        // find <img> where alt == namanya
        return document.selectFirst("div.actress-pic img[alt=\"$namanya\"]")
                ?.attr("src")
                .orEmpty()
    }

        
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("div.bixbox > h1")?.text().orEmpty()
        val poster = document.selectFirst("div.video-container > img")?.attr("src")
                    ?.replace(Regex("(_resized)?\\.webp$"), ".jpg")
                    ?: "Unknown"
        val description = document.selectFirst("div.bixbox > div.right > div > p")
                    ?.text()
                    ?.trim()
                    ?: "Unknown"
        
        //val actors = document.select(""div.bixbox > div.right > div > ul ").take(8).map {
        //            Actor(
        //                    it.text(),
        //                    findposAct(it.text())
        //            )
        //        }
                    
        //val actress = cariArtis(document.select("li:has(strong:matchesOwn(Actress)) a")?.eachText()
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot      = description
            //addActors(actors)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val parts = data.split("#")
        val url = parts[0]
        val epTag = parts.getOrNull(1)
        var found = false

        val doc = app.get(url).document

        if (epTag != null) {
            // ---- SERIES ----
            val epNum = epTag.removePrefix("ep").toIntOrNull()
            val box = doc.select("div.bixbox").getOrNull(epNum?.minus(1) ?: 0)
            val iframe = box?.selectFirst("iframe")?.attr("src")
            if (iframe != null) {
                loadExtractor(iframe, url, subtitleCallback, callback)
                found = true
            }
        } else {
            // ---- MOVIE ----
            // 1. iframe utama
            doc.select("div#embed_holder iframe").forEach { iframe ->
                loadExtractor(iframe.attr("src"), url, subtitleCallback, callback)
                found = true
            }

            // 2. mirror links
            doc.select("ul.mirror li a[data-href]").forEach { a ->
                val mirrorUrl = a.attr("data-href")
                val mirrorDoc = app.get(mirrorUrl).document
                val iframe = mirrorDoc.selectFirst("div#embed_holder iframe")?.attr("src")
                if (iframe != null) {
                    loadExtractor(iframe, mirrorUrl, subtitleCallback, callback)
                    found = true
                }
            }
        }

        return found
    }

    

    
}










