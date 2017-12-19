package co.petrin.kotlin

import org.jetbrains.spek.api.Spek
import org.slf4j.MDC
import ratpack.error.ServerErrorHandler
import ratpack.handling.Context
import ratpack.logging.MDCInterceptor
import ratpack.test.embed.EmbeddedApp

class MDCPreservingSpec : Spek({
    lateinit var embeddedApp: EmbeddedApp

    beforeEachTest {
        embeddedApp = EmbeddedApp.of { app ->
            app.registryOf {
                it.add(MDCInterceptor.instance())
                it.add(ServerErrorHandler::class.java, object: ServerErrorHandler {
                    override fun error(context: Context, throwable: Throwable) {
                        context.response.send(MDC.get("key"))
                    }
                })
            }
            app.handlers { chain ->
                chain.get("simplecall") { ctx ->
                    ctx.async {
                        MDC.put("key", "value")
                        check(MDC.get("key") == "value") { "Values can be obtained from the MDC" }
                        val reply = await { MDC.get("key") } ?: "Key was not present in MDC"
                        ctx.response.send(reply)
                    }
                }

                chain.get("mixedexecution") { ctx ->
                    MDC.put("key", "value")
                    check(MDC.get("key") == "value") { "Values can be obtained from the MDC" }
                    ctx.async {
                        await { Thread.sleep(100)  }
                    }
                    ctx.response.send(MDC.get("key"))
                }

                chain.get("caughtexception") { ctx ->
                    ctx.async {
                        MDC.put("key", "value")
                        check(MDC.get("key") == "value") { "Values can be obtained from the MDC" }
                        try {
                            await { throw IllegalStateException() }
                        } catch (t: Throwable) {
                            val reply = co.petrin.kotlin.await { org.slf4j.MDC.get("key") } ?: "Key was not present in MDC"
                            ctx.response.send(reply)
                        }
                    }
                }

                chain.get("uncaughtexception") { ctx ->
                    ctx.async {
                        MDC.put("key", "value")
                        check(MDC.get("key") == "value") { "Values can be obtained from the MDC" }
                        await { throw IllegalStateException() }
                        ctx.response.send("Error handler was not triggered!")
                    }
                }
            }
        }
    }

    afterEachTest {
        embeddedApp.close()
    }

    test("The MDC is preserved inside await blocks") {
        val response = embeddedApp.httpClient.getText("simplecall")
        check( response == "value") { "Response should have been 'value' but was '$response'" }
    }

    test("The MDC is preserved inside mixed sync/async handling") {
        val response = embeddedApp.httpClient.getText("mixedexecution")
        check( response == "value") { "Response should have been 'value' but was '$response'" }
    }

    test("The MDC is preserved during exception handling") {
        val response = embeddedApp.httpClient.getText("caughtexception")
        check( response == "value") { "Response should have been 'value' but was '$response'" }
    }

    test("The MDC is preserved for the default exception handler") {
        val response = embeddedApp.httpClient.getText("uncaughtexception")
        check( response == "value") { "Response should have been 'value' but was '$response'" }
    }
})
