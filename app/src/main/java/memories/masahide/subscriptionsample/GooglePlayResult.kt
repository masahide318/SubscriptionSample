package memories.masahide.subscriptionsample


/**
 * MyBillingClientのResponseクラスです。
 * 参考以下の2つを参考に作りました
 * - https://github.com/adelnizamutdinov/kotlin-either/blob/master/src/main/kotlin/either/Either.kt
 * - kotlinのResult型
 */
@Suppress("unused")
sealed class GooglePlayResult<out L : GooglePlayException, out R>

class GooglePlayException : Exception()

data class Left(val value: GooglePlayException) : GooglePlayResult<GooglePlayException, Nothing>()
data class Right<out T>(val value: T) : GooglePlayResult<Nothing, T>()

fun <R> GooglePlayResult<GooglePlayException, R>.error(): GooglePlayException {
  if (this is Left) {
    return this.value
  }
  throw Exception()
}

fun <R> GooglePlayResult<GooglePlayException, R>.success(): R {
  if (this is Right) {
    return this.value
  }
  throw Exception()
}

inline fun <R, T> GooglePlayResult<GooglePlayException, R>.fold(
  left: (GooglePlayException) -> T,
  right: (R) -> T
): T =
  when (this) {
    is Left -> left(value)
    is Right -> right(value)
  }

fun <R> GooglePlayResult<GooglePlayException, R>.exceptionOrNull(): GooglePlayException? =
  when (this) {
    is Left -> value
    else -> null
  }

fun <R> GooglePlayResult<GooglePlayException, R>.isSuccess(): Boolean =
  this is Right

fun <R> GooglePlayResult<GooglePlayException, R>.isError(): Boolean =
  this is Left

inline fun <R, T> GooglePlayResult<GooglePlayException, R>.flatMap(f: (R) -> GooglePlayResult<GooglePlayException, T>): GooglePlayResult<GooglePlayException, T> =
  fold({ this as Left }, f)

inline fun <R, T> GooglePlayResult<GooglePlayException, R>.map(f: (R) -> T): GooglePlayResult<GooglePlayException, T> =
  flatMap { Right(f(it)) }

inline fun <R> GooglePlayResult<GooglePlayException, R>.getOrElse(onFailure: (exception: GooglePlayException) -> R): R {
  return when (val exception = exceptionOrNull()) {
    null -> (this as Right).value
    else -> onFailure(exception)
  }
}
