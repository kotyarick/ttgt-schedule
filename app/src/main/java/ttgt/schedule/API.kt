package ttgt.schedule

import android.content.Context
import io.grpc.ManagedChannel
import io.grpc.okhttp.OkHttpChannelBuilder
import java.io.InputStream
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory


fun createGrpcChannel(context: Context): ManagedChannel {
    // Load the certificate from raw resources
    val certInputStream: InputStream = context.resources.openRawResource(R.raw.cert)
    val certificateFactory = CertificateFactory.getInstance("X.509")
    val certificate = certificateFactory.generateCertificate(certInputStream)

    // Create a KeyStore and add the certificate
    val keyStore = java.security.KeyStore.getInstance(java.security.KeyStore.getDefaultType())
    keyStore.load(null, null)
    keyStore.setCertificateEntry("ca", certificate)

    // Create a TrustManager that trusts the certificate
    val trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    trustManagerFactory.init(keyStore)

    // Create an SSLContext and initialize it with the TrustManager
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, trustManagerFactory.trustManagers, null)

    // Build the gRPC channel with the custom SSLContext

    return OkHttpChannelBuilder.forAddress("185.13.47.146", 42069)
        .sslSocketFactory(sslContext.socketFactory)
        .build()
}