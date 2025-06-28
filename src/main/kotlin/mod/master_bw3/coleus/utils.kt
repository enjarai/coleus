package mod.master_bw3.coleus

infix fun <A, B, C> ((A) -> B).then(other: (B) -> C): (A) -> C {
    return { other(this(it)) }
}
