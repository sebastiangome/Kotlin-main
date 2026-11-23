package com.induztek.app.data.repository

import com.induztek.app.domain.model.RolUsuario
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {

    private lateinit var repo: AuthRepositoryImpl

    @Before
    fun setUp() {
        repo = AuthRepositoryImpl()
    }

    @Test
    fun `login con credenciales de tecnico retorna usuario y lo deja como sesion actual`() = runTest {
        val resultado = repo.login("tecnico@induztek.cl", "Induztek2026!")

        assertTrue(resultado.isSuccess)
        assertEquals(RolUsuario.TECNICO, resultado.getOrThrow().rol)
        assertEquals("tecnico@induztek.cl", repo.getUsuarioActual()?.email)
    }

    @Test
    fun `login con credenciales de admin retorna rol ADMIN`() = runTest {
        val resultado = repo.login("admin@induztek.cl", "Admin2026!")

        assertEquals(RolUsuario.ADMIN, resultado.getOrThrow().rol)
    }

    @Test
    fun `el correo no distingue mayusculas`() = runTest {
        assertTrue(repo.login("TECNICO@Induztek.CL", "Induztek2026!").isSuccess)
    }

    @Test
    fun `correo incorrecto con contrasena correcta falla y no abre sesion`() = runTest {
        val resultado = repo.login("correo.malo@induztek.cl", "Induztek2026!")

        assertTrue(resultado.isFailure)
        assertTrue(resultado.exceptionOrNull()!!.message!!.startsWith("Credenciales incorrectas"))
        assertNull(repo.getUsuarioActual())
        assertNull(repo.currentUsuario.value)
    }

    @Test
    fun `contrasena incorrecta con correo correcto falla con el mismo mensaje`() = runTest {
        val malCorreo = repo.login("correo.malo@induztek.cl", "Induztek2026!")
        val malClave = repo.login("tecnico@induztek.cl", "Incorrecta2026!")

        assertTrue(malClave.isFailure)
        assertEquals(malCorreo.exceptionOrNull()!!.message, malClave.exceptionOrNull()!!.message)
        assertNull(repo.getUsuarioActual())
    }

    @Test
    fun `la contrasena de un usuario no sirve para otro`() = runTest {
        assertTrue(repo.login("tecnico@induztek.cl", "Admin2026!").isFailure)
    }

    @Test
    fun `logout limpia la sesion`() = runTest {
        repo.login("tecnico@induztek.cl", "Induztek2026!")
        repo.logout()

        assertNull(repo.getUsuarioActual())
    }
}
