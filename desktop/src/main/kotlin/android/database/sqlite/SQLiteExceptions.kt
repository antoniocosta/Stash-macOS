package android.database.sqlite

/*
 * Android's SQLite exception hierarchy (same FQNs). Note: Room's bundled
 * driver on desktop throws androidx.sqlite.SQLiteException, so upstream
 * catch sites for these types are compile-only on desktop (see PORTING.md).
 */
open class SQLiteException : android.database.SQLException {
    constructor() : super()
    constructor(error: String?) : super(error)
    constructor(error: String?, cause: Throwable?) : super(error, cause)
}

open class SQLiteConstraintException : SQLiteException {
    constructor() : super()
    constructor(error: String?) : super(error)
}
