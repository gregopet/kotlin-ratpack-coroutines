package co.petrin.kotlin

import kotlinx.coroutines.experimental.*
import ratpack.exec.Blocking

/**
 * Runs the given block on the request thread.
 *
 * Frees the request thread as soon as the first <tt>await<tt> (or any other suspendable function) is encountered and
 * continues work on another appropriate thread after the blocking operation finishes.
 *
 * @param block The block to execute
 */
inline fun async(noinline block: suspend () -> Any) {
   launch(Unconfined, CoroutineStart.UNDISPATCHED) {
      block()
   }
}

/**
 * Returns the result of the [block] as soon as the blocking computation completes. The request thread is released
 * during the blocking operation.
 */
suspend fun <T> await(block: () -> T): T = suspendCancellableCoroutine { cont: CancellableContinuation<T> ->
   Blocking.get(block)
   .onError{
      cont.resumeWithException(it)
   }
   .then {
      cont.resume(it)
   }
}