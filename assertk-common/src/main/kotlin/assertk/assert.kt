package assertk

import assertk.assertions.support.show

/**
 * Marks the assertion DSL.
 */
@DslMarker
annotation class AssertkDsl

/**
 * An assertion. Holds an actual value to assertion on and an optional name.
 * @see [assert]
 */
@AssertkDsl
class Assert<out T> internal constructor(val actual: T, val name: String?, internal val context: Any?) {
    /**
     * Asserts on the given value with an optional name.
     *
     * ```
     * assert(true, name = "true").isTrue()
     * ```
     */
    fun <R> assert(actual: R, name: String? = this.name)
            : Assert<R> = Assert(actual, name, if (context != null || this.actual === actual) context else this.actual)
}

/**
 * An assertion on a block of code. Can assert that it either throws and error or returns a value.
 */
sealed class AssertBlock<out T> {
    /**
     * Runs the given lambda if the block throws an error, otherwise fails.
     */
    abstract fun thrownError(f: Assert<Throwable>.() -> Unit)

    /**
     * Runs the given lambda if the block returns a value, otherwise fails.
     */
    abstract fun returnedValue(f: Assert<T>.() -> Unit)

    abstract fun doesNotThrowAnyException()

    internal class Value<out T> internal constructor(private val value: T) : AssertBlock<T>() {
        override fun thrownError(f: Assert<Throwable>.() -> Unit) = fail("expected exception but was:${show(value)}")

        override fun returnedValue(f: Assert<T>.() -> Unit) {
            f(assert(value))
        }

        override fun doesNotThrowAnyException() {
            assert(value)
        }
    }

    internal class Error<out T> internal constructor(private val error: Throwable) : AssertBlock<T>() {
        override fun thrownError(f: Assert<Throwable>.() -> Unit) {
            f(assert(error))
        }

        override fun returnedValue(f: Assert<T>.() -> Unit) = fail("expected value but threw:${show(error)}")

        override fun doesNotThrowAnyException() = fail("expected to not throw an exception but threw:${show(error)}")
    }
}

/**
 * Asserts on the given value with an optional name.
 *
 * ```
 * assert(true, name = "true").isTrue()
 * ```
 */
fun <T> assert(actual: T, name: String? = null): Assert<T> = Assert(actual, name, null)

/**
 * All assertions in the given lambda are run.
 *
 * ```
 * assert("test", name = "test").all {
 *   startsWith("t")
 *   endsWith("t")
 * }
 * ```
 */
fun <T> Assert<T>.all(f: Assert<T>.() -> Unit) {
    FailureContext.run(SoftFailure()) {
        f()
    }
}

/**
 * Asserts on the given block. You can test that it returns a value or throws an exception.
 *
 * ```
 * assert { 1 + 1 }.returnedValue {
 *   isPositive()
 * }
 *
 * assert {
 *   throw Exception("error")
 * }.thrownError {
 *   hasMessage("error")
 * }
 * ```
 */
fun <T> assert(f: () -> T): AssertBlock<T> {
    return FailureContext.run(SoftFailure()) {
        @Suppress("TooGenericExceptionCaught")
        try {
            AssertBlock.Value(f())
        } catch (e: Throwable) {
            AssertBlock.Error(e)
        }
    }
}

/**
 * Runs all assertions in the given lambda and reports any failures.
 */
fun assertAll(f: () -> Unit) {
    FailureContext.run(SoftFailure(), f)
}

/**
 * Catches any exceptions thrown in the given lambda and returns it. This is an easy way to assert on expected thrown
 * exceptions.
 *
 * ```
 * val exception = catch { throw Exception("error") }
 * assert(exception).isNotNull {
 *   hasMessage("error")
 * }
 * ```
 */
fun catch(f: () -> Unit): Throwable? {
    @Suppress("TooGenericExceptionCaught")
    try {
        f()
        return null
    } catch (e: Throwable) {
        return e
    }
}

/**
 * Runs the given lambda if the block throws expected error, otherwise fails.
 */
inline fun <reified E : Throwable> AssertBlock<Any>.thrownExpectedError(crossinline f: Assert<E>.() -> Unit) {
    thrownError {
        if (this.actual is E) {
            @Suppress("UNCHECKED_CAST")
            f.invoke(this as Assert<E>)
        } else {
            fail("expected Throwable to be of type ${show(E::class)} but actual was ${show(actual::class)}")
        }
    }
}
