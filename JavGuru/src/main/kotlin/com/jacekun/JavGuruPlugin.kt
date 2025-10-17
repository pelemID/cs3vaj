package com.jacekun

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class JavGuruPlugin: Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerMainAPI(JavGuru())
        registerExtractorAPI(DoodJav())
        registerExtractorAPI(javclan())
        registerExtractorAPI(Streamwish())
        registerExtractorAPI(Maxstream())
        registerExtractorAPI(Vidhidepro())
        registerExtractorAPI(Vidhidepro())
        registerExtractorAPI(Ds2Play())
        registerExtractorAPI(Streamhihi())
        registerExtractorAPI(Javlion())
        registerExtractorAPI(VidhideVIP())
        registerExtractorAPI(Javsw())
        registerExtractorAPI(swhoi())
        registerExtractorAPI(MixDropis())
        registerExtractorAPI(Javmoon())
        registerExtractorAPI(d000d())
    }
}
