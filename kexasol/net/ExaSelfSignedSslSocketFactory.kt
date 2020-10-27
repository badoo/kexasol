package com.badoo.kexasol.net

import com.badoo.kexasol.ExaConnectionOptions
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.InputStream
import java.math.BigInteger
import java.net.Socket
import java.security.*
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.*


internal class ExaSelfSignedSslSocketFactory(val options: ExaConnectionOptions) {
    private val bcProvider = BouncyCastleProvider()
    private val keyPair: KeyPair
    private val cert: X509Certificate
    private val socketFactory: SSLSocketFactory

    init {
        Security.addProvider(bcProvider)

        keyPair = generateKeyPair()
        cert = generateSelfSignedCertificate()
        socketFactory = createSocketFactory()
    }

    fun wrapSocket(socket: Socket, stream: InputStream): SSLSocket {
        val sslSocket =
            socketFactory.createSocket(socket, stream, true) as SSLSocket

        sslSocket.useClientMode = false
        sslSocket.enabledProtocols = arrayOf("TLSv1.2", "TLSv1.3")

        return sslSocket
    }

    private fun generateKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA", bcProvider)
        generator.initialize(2048)

        return generator.generateKeyPair()
    }

    private fun generateSelfSignedCertificate(): X509Certificate {
        val contentSigner = JcaContentSignerBuilder("SHA256WITHRSA")
            .build(keyPair.private)

        val x500Name = X500Name("CN=${options.clientName}")

        val certBuilder = JcaX509v3CertificateBuilder(
            x500Name,
            BigInteger.ONE,
            Date(),
            Date(System.currentTimeMillis() + 7776000L),
            x500Name,
            keyPair.public
        )

        return JcaX509CertificateConverter()
            .setProvider(bcProvider)
            .getCertificate(certBuilder.build(contentSigner))
    }

    private fun createSocketFactory(): SSLSocketFactory {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null)
        keyStore.setKeyEntry("ca", keyPair.private, null, arrayOf(cert))

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, null)

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, SecureRandom())

        return sslContext.socketFactory
    }
}
