package com.JavID

import android.util.Base64
import com.lagradost.api.Log
//import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors

class Javsubid : MainAPI() {
    override var mainUrl              = "https://javsubid.sx"
    override var name                 = "Javsubid"
    override val hasMainPage          = true
    override var lang                 = "id"
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        // Fixed entries
            "category/jav-sub-indo?filter=latest" to "Terbaru",
            "category/jav-sub-indo?filter=most-viewed" to "Populer",
            "category/jav-sub-indo?filter=longest" to "Cerita Panjang"
            "category/jav-sub-indo?filter=random" to "Acak",
    )
    
    
	override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        
		val baseUrl = request.data.substringBefore("?")
		val query = request.data.substringAfter("?", "")
		
		val document = app.get("$mainUrl/$baseUrl/page/$page?$query").document
        val home = document.select("#main > div.videos-list")
            .mapNotNull { it.toSearchResult() }
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
        val title = this.select("article > a").attr("title")
        val href = fixUrl(this.select("article > a").attr("href"))
        val posterUrl = fixUrlNull(this.select("article > a > div > div > img").attr("data-src"))
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


    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title= document.selectFirst("div.title-block box-shadow > h1")?.text().toString()
        val poster = document.selectFirst("div.video-player > meta:nth-child(5)") 
                    ?.attr("content")?.trim().orEmpty()
        val description = document.selectFirst("div.video-player > meta:nth-child(3)")
                    ?.attr("content")?.trim().orEmpty()

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
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
