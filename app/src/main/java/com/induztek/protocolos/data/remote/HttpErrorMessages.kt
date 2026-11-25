package com.induztek.protocolos.data.remote

/** Mensaje legible para el usuario según el código HTTP que devuelven los microservicios. */
fun httpErrorMessage(code: Int): String = when (code) {
    400 -> "Los datos enviados no son válidos"
    401 -> "Tu sesión expiró o las credenciales son incorrectas"
    403 -> "No tienes permisos para realizar esta acción"
    404 -> "No se encontró el recurso solicitado"
    409 -> "El registro ya existe o entra en conflicto con otro"
    502, 503, 504 -> "El servicio no está disponible por ahora. Intenta nuevamente más tarde"
    in 500..599 -> "Error del servidor ($code)"
    else -> "Error inesperado del servidor ($code)"
}
