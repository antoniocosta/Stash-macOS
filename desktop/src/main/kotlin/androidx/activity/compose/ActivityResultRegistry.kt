package androidx.activity.compose

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import javax.swing.SwingUtilities

/**
 * activity-compose's rememberLauncherForActivityResult. `launch` resolves the contract on the UI
 * thread after the current event (like Android, the result is delivered asynchronously, never
 * re-entrantly inside the caller) and hands the result to the latest [onResult].
 */
@Composable
fun <I, O> rememberLauncherForActivityResult(
    contract: ActivityResultContract<I, O>,
    onResult: (O) -> Unit,
): ManagedActivityResultLauncher<I, O> {
    val currentContract = rememberUpdatedState(contract)
    val currentOnResult = rememberUpdatedState(onResult)
    return remember { ManagedActivityResultLauncher(currentContract, currentOnResult) }
}

class ManagedActivityResultLauncher<I, O> internal constructor(
    private val contract: State<ActivityResultContract<I, O>>,
    private val onResult: State<(O) -> Unit>,
) : ActivityResultLauncher<I>() {
    val contractValue: ActivityResultContract<I, O> get() = contract.value

    override fun launch(input: I) {
        SwingUtilities.invokeLater {
            val result = contract.value.resolve(input)
            onResult.value(result)
        }
    }
}
