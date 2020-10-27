package com.badoo.kexasol.net

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession

internal class ExaNoCertHostnameVerifier : HostnameVerifier {
    override fun verify(hostname: String?, session: SSLSession?): Boolean {
        return true
    }
}
