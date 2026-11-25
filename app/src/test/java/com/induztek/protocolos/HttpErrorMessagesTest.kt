package com.induztek.protocolos

import com.induztek.protocolos.data.remote.httpErrorMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpErrorMessagesTest {

    @Test
    fun codigosDeCliente_tienenMensajeEspecifico() {
        assertEquals("Los datos enviados no son válidos", httpErrorMessage(400))
        assertEquals("No tienes permisos para realizar esta acción", httpErrorMessage(403))
        assertEquals("No se encontró el recurso solicitado", httpErrorMessage(404))
        assertTrue(httpErrorMessage(409).contains("ya existe"))
    }

    @Test
    fun codigosDeServicioNoDisponible_comparteMensaje() {
        val mensaje = httpErrorMessage(503)
        assertEquals(mensaje, httpErrorMessage(502))
        assertEquals(mensaje, httpErrorMessage(504))
    }

    @Test
    fun otrosErroresDelServidor_incluyenElCodigo() {
        assertEquals("Error del servidor (500)", httpErrorMessage(500))
        assertTrue(httpErrorMessage(418).contains("418"))
    }
}
