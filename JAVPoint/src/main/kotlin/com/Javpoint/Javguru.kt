package com.Javpoint

import android.util.Base64
import com.lagradost.api.Log
//import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors

class Javguru : MainAPI() {
    override var mainUrl              = "https://jav.guru/"
    override var name                 = "Javguru"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        // Fixed entries
            "category/english-subbed" to "English Subbed",
            "tag/for-women" to "For Women",
            "actress/hanamiya-amu" to "Amu",

        // Random 3 every refresh
            *listOf(
                "category/jav-uncensored" to "Uncensored",
                "category/amateur" to "Amateur",
                "category/fc2" to "FC2",
                "category/idol" to "Idol",
                "tag/drama" to "Drama",
                "/tag/debut-production" to "Debut",
                "tag/masturbation" to "Masturbation",
                "tag/married-woman" to "Married Woman",
                "tag/mature-woman" to "Mature Woman",
                "tag/best-of-2016" to "Best of 2016",
                "tag/best-of-2017" to "Best of 2017",
                "tag/best-of-2018" to "Best of 2018",
                "tag/best-of-2019" to "Best of 2019",
                "tag/best-of-2020" to "Best of 2020",
                "tag/best-of-2021" to "Best of 2021",
                "tag/best-of-2022" to "Best of 2022",
                "tag/best-of-2023" to "Best of 2023",
                "tag/best-of-2024" to "Best of 2024",
                "studio/ipzz" to "IdeaPocket",
                "studio/mida" to "Moodyz",
                "series/身も心も相性抜群の2人-。想いと唇が重な" to "Compatibility",
                "series/超高級中出し専門ソープ" to "Soapland",
            ).shuffled().take(3).toTypedArray()
    )

    
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/${request.data}/page/$page").document
        val home = document.select("#main > div")
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
        val title     = this.select("div > div > div > a > img").attr("alt")
        val href      = fixUrl(this.select("div > div > div > a").attr("href"))
        val posterUrl = fixUrlNull(this.select("div > div > div > a > img").attr("src"))
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

/*
    private fun findposAct(query: String) {
        //pisahin dulu keyword nama

        //masukin ke kueri
        val document = app.get("${mainUrl}/actress-search/?taxonomy_search=$kueri1+kueri2+kueri3").document

    }
*/
        
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title= document.selectFirst("div.posts > h1")?.text().toString()
        val poster = document.selectFirst("div.wp-content > p > img")
                    ?.attr("src")
                    ?.trim()
                    ?: document.selectFirst("div.large-screenimg > img")
                    ?.attr("src")
                    ?.trim()
                    .orEmpty()
        val description = document.selectFirst("div.wp-content p")
                    ?.text()
                    ?.trim()
                    ?: document.select("li:has(strong:matchesOwn(Actress)) a")
                    ?.eachText()
                    ?.joinToString(", ")
                    .orEmpty()

         val actors = document.select("li:has(strong:matchesOwn(Actress)) a").map {
                    Actor(
                            it.text(), it.text()
                            //findposAct(it.text)
                    )
                }
                    
        //val actress = cariArtis(document.select("li:has(strong:matchesOwn(Actress)) a")?.eachText()
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot      = description
            addActors(actors)
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
