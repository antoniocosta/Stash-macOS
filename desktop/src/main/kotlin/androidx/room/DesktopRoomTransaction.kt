package androidx.room

/*
 * Desktop port of room-ktx's Android-only RoomDatabase.withTransaction.
 * Uses Room's multiplatform connection API; DAO calls made inside [block]
 * run on the same writer connection/transaction (Room propagates the
 * connection through the coroutine context), matching Android semantics.
 */
suspend fun <R> RoomDatabase.withTransaction(block: suspend () -> R): R =
    useWriterConnection { transactor ->
        transactor.immediateTransaction { block() }
    }
