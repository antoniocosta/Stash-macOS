package android.accounts

/** Desktop shim for android.accounts.Account (a name/type pair). */
data class Account(@JvmField val name: String, @JvmField val type: String)
