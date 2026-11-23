package com.induztek.app.domain.usecase

import com.induztek.app.domain.model.RolUsuario
import com.induztek.app.domain.model.Usuario
import com.induztek.app.domain.policy.PasswordPolicy
import com.induztek.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ValidarLoginUseCaseTest {

    private lateinit var repo: FakeAuthRepository
    private lateinit var useCase: ValidarLoginUseCase

    @Before
    fun setUp() {
        repo = FakeAuthRepository()
        useCase = ValidarLoginUseCase(repo, PasswordPolicy())
    }

    @Test
    fun `correo con formato malo y contrasena valida falla sin llamar al repositorio`() = runTest {
        val resultado = useCase("correo-malo", "Induztek2026!")

        assertTrue(resultado.isFailure)
        assertEquals(
            "El formato del correo electrónico no es válido",
            resultado.exceptionOrNull()?.message
        )
        assertFalse(repo.loginLlamado)
    }

    @Test
    fun `correo incompleto sin dominio completo tambien es invalido`() = runTest {
        listOf("tecnico@", "tecnico@induztek", "@induztek.cl").forEach { correo ->
            val resultado = useCase(correo, "Induztek2026!")
            assertTrue("Debio fallar: $correo", resultado.isFailure)
        }
        assertFalse(repo.loginLlamado)
    }

    @Test
    fun `correo vacio falla`() = runTest {
        val resultado = useCase("   ", "Induztek2026!")
        assertTrue(resultado.isFailure)
        assertEquals("El correo electrónico es requerido", resultado.exceptionOrNull()?.message)
    }

    @Test
    fun `correo valido y contrasena valida delega al repositorio`() = runTest {
        val resultado = useCase("  tecnico@induztek.cl ", "Induztek2026!")

        assertTrue(resultado.isSuccess)
        assertTrue(repo.loginLlamado)
        assertEquals("tecnico@induztek.cl", repo.ultimoEmail)
    }

    private class FakeAuthRepository : AuthRepository {
        var loginLlamado = false
        var ultimoEmail: String? = null
        private val _current = MutableStateFlow<Usuario?>(null)
        override val currentUsuario: StateFlow<Usuario?> = _current

        override suspend fun login(email: String, password: String): Result<Usuario> {
            loginLlamado = true
            ultimoEmail = email
            return Result.success(Usuario(1L, email, "Test", RolUsuario.TECNICO, "token"))
        }

        override suspend fun logout() {}
        override suspend fun getUsuarioActual(): Usuario? = _current.value
    }
}
