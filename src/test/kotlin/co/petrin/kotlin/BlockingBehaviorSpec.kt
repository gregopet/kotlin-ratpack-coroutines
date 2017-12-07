package co.petrin.kotlin

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.*
import ratpack.test.embed.EmbeddedApp
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread

private val threadName get() = "(thread: ${Thread.currentThread()}"


class BlockingBehaviorSpec : Spek({
    given("An embedded app with a blocking & non-blocking operation") {
        var semaphore: Semaphore? = null
        var embeddedApp: EmbeddedApp? = null

        beforeEachTest {
            semaphore = Semaphore(1)
            embeddedApp = EmbeddedApp.of {
                it.serverConfig { it.threads(1) }
                it.handlers { chain ->

                    chain.get("sleepblock") { ctx ->
                        println("INSIDE SLEEPBLOCK HANDLER $threadName")
                        semaphore!!.release()
                        Thread.sleep(1000 * 5)
                        println("EXITING SLEEPBLOCK HANDLER $threadName")
                        ctx.response.send("SLEEP")
                    }

                    chain.get("sleep") { ctx ->
                        println("INSIDE SLEEP HANDLER $threadName")
                        semaphore!!.release()

                        async {
                            println("SLEEPING ASYNC $threadName")
                            var message = await {
                                println("SLEEPING BLOCK ASYNC $threadName")
                                Thread.sleep(1000 * 5)
                                "WOKE UP"
                            }
                            println("SLEEPING ASYNC OVER $threadName")
                            ctx.response.send(message)
                        }
                        println("EXITING SLEEP HANDLER $threadName")

                    }

                    chain.get("quick") {
                        println("INSIDE QUICK HANDLER $threadName")
                        Thread.sleep(100)
                        it.response.send("ECHO $threadName")
                    }

                    chain.post("echobody") { ctx ->
                        println("INSIDE ECHOBODY HANDLER $threadName")
                        async {
                            println("INSIDE ECHOBODY ASYNC $threadName")
                            val txt = ctx.request.body.await().text
                            println("INSIDE ECHOBODY ASYNC AFTER COROUTINE $threadName")
                            ctx.response.send(txt)
                        }
                    }
                }
            }
        }

        afterEachTest {
            embeddedApp!!.close()
        }

        test("A blocking function will clog our test app") {
            var blockingSleepFinished = false
            var simpleRequestFinished = false
            semaphore!!.acquire()
            thread(start = true, isDaemon = true) {
                try {
                    embeddedApp!!.httpClient.getText("sleepblock").let(::println)
                    blockingSleepFinished = true
                } catch (x: Exception) {}
            }

            semaphore!!.acquire()

            thread(start = true, isDaemon = true) {
                semaphore!!.release()
                embeddedApp!!.httpClient.getText("quick").let(::println)
                simpleRequestFinished = true
            }
            semaphore!!.acquire()
            Thread.sleep(1000)

            check(!blockingSleepFinished) { "Blocking should still be computing" }
            check(!simpleRequestFinished) { "Quick thread should be waiting on the blocking head"}
        }


        test("A non-blocking function will not clog our test app") {

            var blockingSleepFinished = false
            var simpleRequestFinished = false


            println("")
            semaphore!!.acquire()
            thread(start = true, isDaemon = true) {
                try {
                    val response = embeddedApp!!.httpClient.getText("sleep")
                    check(response == "WOKE UP")
                    blockingSleepFinished = true
                } catch (x: Exception) {}
            }
            semaphore!!.acquire()

            thread(start = true, isDaemon = true) {
                try {
                    semaphore!!.release()
                    embeddedApp!!.httpClient.getText("quick").let(::println)
                    simpleRequestFinished = true
                } catch (x: Exception) {}
            }
            semaphore!!.acquire()
            Thread.sleep(1000)

            check(!blockingSleepFinished)  { "Blocking should still be computing" }
            check(simpleRequestFinished)  { "Quick thread should NOT be waiting on the blocking head"}
        }

        test("A non-blocking function will return the correct result") {
            check(embeddedApp!!.httpClient.getText("sleep") == "WOKE UP")
        }

        test("await() can be called on Ratpack promises") {
            val requestWithBody = embeddedApp!!.httpClient.requestSpec { it.body.text("foobar") }
            val response = requestWithBody.postText("echobody")
            check(response == "foobar") { "Response $response should be 'foobar'" }
        }
    }
})

