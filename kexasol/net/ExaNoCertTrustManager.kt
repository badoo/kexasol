package com.badoo.kexasol.net

import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

internal class ExaNoCertTrustManager : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        //do nothing
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        //do nothing
    }

    override fun getAcceptedIssuers(): Array<X509Certificate?>? {
        return arrayOfNulls(0)
    }
}
