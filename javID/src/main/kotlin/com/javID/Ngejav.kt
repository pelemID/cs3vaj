package com.JavID

import android.util.Base64
import com.lagradost.api.Log
//import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors

class Ngejav : MainAPI() {
    override var mainUrl              = "https://ngejav.life"
    override var name                 = "Ngejav"
    override val hasMainPage          = true
    override var lang                 = "id"
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        // Fixed entries
            "jav-sub-indo" to "Sub Indo"
    )
    
    
	override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        
		val document = app.get("$mainUrl/${request.data}/?page=$page").document
	    val home = document.select("table.postable > tbody > tr:nth-child(odd)")
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

//		#search-form > div.lapislist > div > table > tbody > tr:nth-child(1) > td:nth-child(1)
//		#search-form > div.lapislist > div > table > tbody > tr:nth-child(1) > td:nth-child(2)

    private fun Element.toSearchResult(): SearchResponse {
        val title = this.select("td:nth-child(1) > img").attr("title")
		val href = this.select("td:nth-child(2) > a").attr("href")
        val posterUrl = this.select("td:nth-child(1) > img").attr("src")
        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..3) {
            val document = app.get("${mainUrl}/search?q=$query&page=$i").document
            val results = document.select("table.postable > tbody > tr").mapNotNull { it.toSearchResult() }
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

        val title= document.selectFirst("h1.kategori")?.text().toString()
		val poster = document.selectFirst("div.content-wrapper > center:nth-child(3) > img") 
                    ?.attr("src")?.trim().orEmpty()
		val description = document.selectFirst("div.content-wrapper > div.lister > table > tbody > tr:nth-child(3) > td:nth-child(2)")
					?.text()?.trim().orEmpty()

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
        }
    }

     override suspend fun loadLinks(
		data: String,
    	isCasting: Boolean,
    	subtitleCallback: (SubtitleFile) -> Unit,
    	callback: (ExtractorLink) -> Unit
	 ): Boolean {
        val document = app.get(data).document
        document.select("div.iframe-wrapper iframe").forEachIndexed { index, iframe ->
            val src = iframe.attr("src")
            val link = if ("lelebakar.xyz" in src) {
				app.get(src).document
                    .selectFirst("script:containsData(p,a,c,k,e,d)")?.data()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { JsUnpacker(it).unpack() }
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { Regex("file:\"(.*?)\"").find(it)?.groupValues?.getOrNull(1) }
            } else {
                app.get(src).document.selectFirst("script:containsData(urlPlay)")?.data()
                    ?.let { Regex("urlPlay\\s*=\\s*'(.*?)'").find(it)?.groupValues?.getOrNull(1) }
            }
            link?.let {
                callback.invoke(
                    newExtractorLink(
                        source = "$name $index",
                        name = "$name $index",
                        url = it,
                        ExtractorLinkType.M3U8
                    ) {
                        this.referer = ""
                        this.quality = Qualities.Unknown.value
                    }
                )
            }

		loadExtractor(link, referer = src, subtitleCallback, callback)
        }

        return true
    }

}
