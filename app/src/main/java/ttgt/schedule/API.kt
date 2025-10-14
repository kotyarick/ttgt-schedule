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

//object API : Cloneable {
//    lateinit var stub: ServerGrpc.ServerStub // = ServerGrpc.newStub(createGrpcChannel())
//
//    suspend fun groups(): Map<String, String> = http.get("/groups").parse()
//
//    suspend fun schedule(group: String): Schedule {
//        val json: List<List<List<JsonObject?>>> = http.get("/schedule/$group").parse()
//
//        return Schedule.newBuilder()
//            .addAllWeeks(
//                json.map {
//                    Week.newBuilder()
//                        .addAllDays(
//                            it.map {
//                                Day.newBuilder()
//                                    .addAllLesson(
//                                        it.map { jsonElement ->
//                                            val builder = ttgt.schedule.proto.Lesson.newBuilder()
//
//                                            if (jsonElement == null) return@map builder.setNoLesson(Empty.newBuilder().build()).build()
//
//                                            if (jsonElement.containsKey("subgroups")) {
//                                                return@map builder.setSubgroupedLesson(
//                                                    SubgroupedLesson.newBuilder()
//                                                        .setName(jsonElement["name"]!!.jsonPrimitive.content)
//                                                        .addAllSubgroups(jsonElement["subgroups"]!!.jsonArray.map {
//                                                            SubgroupedLessonData.newBuilder()
//                                                                .setRoom(it.jsonObject["room"]!!.jsonPrimitive.content)
//                                                                .setTeacher(it.jsonObject["teacher"]!!.jsonPrimitive.content)
//                                                                .build()
//                                                        })
//                                                        .build()
//                                                ).build()
//                                            } else {
//                                                return@map builder
//                                                    .setCommonLesson(
//                                                        CommonLesson.newBuilder()
//                                                            .setName(jsonElement["name"]!!.jsonPrimitive.content)
//                                                            .setRoom(jsonElement["room"]!!.jsonPrimitive.content)
//                                                            .setTeacher(jsonElement["teacher"]!!.jsonPrimitive.content)
//                                                            .build()
//                                                    )
//                                                    .build()
//                                            }
//                                        }
//                                    )
//                                    .build()
//                            }
//                        )
//                        .build()
//
//                }
//            )
//            .build()
//    }
//}