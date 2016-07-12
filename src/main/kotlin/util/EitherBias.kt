package util

import org.funktionale.either.Either
import org.funktionale.either.flatMap

fun <L, R, NR> Either<L, R>.map(f: (R) -> NR) = this.right().map(f)

fun <L, R, NR> Either<L, R>.flatMap(f: (R) -> Either<L, NR>) = this.right().flatMap(f)