package com.induztek.protocolos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.induztek.protocolos.data.remote.httpErrorMessage
import retrofit2.HttpException
import java.io.IOException

internal fun ViewModel.launchCatching(
    onError: (String) -> Unit,
    block: suspend CoroutineScope.() -> Unit
): Job {
    val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        if (exception !is CancellationException) onError(exception.toRemoteMessage())
    }
    return viewModelScope.launch(context = exceptionHandler, block = block)
}

private fun Throwable.toRemoteMessage(): String = when (this) {
    is HttpException -> httpErrorMessage(code())
    is IOException -> "No fue posible conectar con el servidor. Verifica tu conexión."
    else -> localizedMessage ?: "Ocurrió un error inesperado"
}
