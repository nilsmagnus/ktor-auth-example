import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.auth.AuthProvider
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.request.header
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    val server = embeddedServer(
            Netty,
            port = 8080,
            module = Application::mymodule
    ).apply {
        start(wait = false)
    }

    runBlocking {
        val client = HttpClient(Apache) {
            install(Auth) {
                customAuth {
                    customAuthHeaderValue = "00-11-22-33-44-55-66"
                }
            }
        }

        val message = client.post<String> {
            url("http://127.0.0.1:8080/")
            body = "Hei du der"
        }

        println("CLIENT: Message from the server: $message")

        client.close()
        server.stop(1L, 1L, TimeUnit.SECONDS)
    }
}

fun Application.mymodule() {
    install(ContentNegotiation) {
    }
    routing {
        post("/") {
            val message = call.receive<String>()
            println("SERVER: Message from the client: $message")
            call.respond("Your authheader is " + call.request.header(HttpHeaders.Authorization))
        }
    }
}

fun Auth.customAuth(block: OauthConfig.() -> Unit) {
    with(OauthConfig().apply(block)) {
        providers.add(OAuthProvider(customAuthHeaderValue))
    }
}

class OauthConfig {
    lateinit var customAuthHeaderValue: String
}

class OAuthProvider(
        private val customAuthHeaderValue: String

) : AuthProvider {


    override fun isApplicable(auth: HttpAuthHeader): Boolean {
        return true
    }

    override suspend fun addRequestHeaders(request: HttpRequestBuilder) {
        request.headers[HttpHeaders.Authorization] = constructBearerHeader()
    }

    fun constructBearerHeader(): String {
        return "Custom $customAuthHeaderValue"
    }

}

