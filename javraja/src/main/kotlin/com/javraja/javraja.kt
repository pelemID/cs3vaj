package com.javraja

import android.util.Base64
import com.lagradost.api.Log
//import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors

class javraja : MainAPI() {
    override var mainUrl              = "https://rajajav.com"
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
        val document = app.get("$mainUrl/${request.data}/").document
        val home =  document.select("div.bx").map { it.toSearchResult() }
        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
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

        val title= document.selectFirst("div.bixbox > h1")?.text().toString()
        val poster = document.selectFirst("div.video-container > img").attr("src").replace(Regex("(_resized)?\\.webp$"), ".jpg").orEmpty()
        val description = document.selectFirst("div.bixbox > div.right > div > p")?.text()?.trim().orEmpty()

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

     override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        val script=document.select("script:containsData(iframe_url)").html()
        val IFRAME_B64_REGEX = Regex(""""iframe_url":"([^"]+)"""")
         val iframeUrls = IFRAME_B64_REGEX.findAll(script)
             .map { it.groupValues[1] }
             .map { Base64.decode(it, Base64.DEFAULT).let(::String) }
             .toList()
         iframeUrls.forEach {
             Log.d("Phisher",it)
             val iframedoc=app.get(it, referer = it).document
             val olid=iframedoc.toString().substringAfter("var OLID = '").substringBefore("'")
             val newreq=iframedoc.toString().substringAfter("iframe").substringAfter("src=\"").substringBefore("'+OLID")
             val reverseid= olid.edoceD()
             val location= app.get("$newreq$reverseid", referer = it, allowRedirects = false)
             val link=location.headers["location"].toString()
             if (link.contains(".m3u"))
             {
                 callback.invoke(
                     newExtractorLink(
                         source = name,
                         name = name,
                         url = link,
                         INFER_TYPE
                     ) {
                         this.referer = ""
                         this.quality = getQualityFromName("")
                     }
                 )
             }
             else{
                 loadExtractor(link, referer = it,subtitleCallback,callback)
             }
         }
        return true
    }

    fun String.edoceD(): String {
        var x = this.length - 1
        var edoceD = ""
        while (x >= 0) {
            edoceD += this[x]
            x--
        }
        return edoceD
    }

}



