package com.induztek.app.presentation.screen.login.viewmodel

import com.induztek.app.data.repository.AuthRepositoryImpl
import com.induztek.app.domain.model.RolUsuario
import com.induztek.app.domain.policy.PasswordPolicy
import com.induztek.app.domain.usecase.ValidarLoginUseCase
import com.induztek.app.presentation.screen.login.state.LoginEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = LoginViewModel(ValidarLoginUseCase(AuthRepositoryImpl(), PasswordPolicy()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `correo con formato malo y contrasena valida muestra error y no navega`() = runTest {
        val efectos = mutableListOf<LoginEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.collect { efectos.add(it) } }

        viewModel.onEmailChanged("correo-malo")
        viewModel.onPasswordChanged("Induztek2026!")
        viewModel.login()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("El formato del correo electrónico no es válido", state.errorMessage)
        assertFalse(state.isLoading)
        assertTrue(efectos.isEmpty())
        job.cancel()
    }

    @Test
    fun `correo con formato valido pero no registrado muestra credenciales incorrectas`() = runTest {
        viewModel.onEmailChanged("otro@induztek.cl")
        viewModel.onPasswordChanged("Induztek2026!")
        viewModel.login()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage!!.startsWith("Credenciales incorrectas"))
    }

    @Test
    fun `contrasena que incumple la politica se informa antes de autenticar`() = runTest {
        viewModel.onEmailChanged("tecnico@induztek.cl")
        viewModel.onPasswordChanged("corta")
        viewModel.login()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.errorMessage!!.startsWith("Credenciales"))
    }

    @Test
    fun `credenciales validas emiten NavigateToHome con el rol del usuario`() = runTest {
        val efectos = mutableListOf<LoginEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.collect { efectos.add(it) } }

        viewModel.onEmailChanged("admin@induztek.cl")
        viewModel.onPasswordChanged("Admin2026!")
        viewModel.login()
        advanceUntilIdle()

        assertEquals(listOf<LoginEffect>(LoginEffect.NavigateToHome(RolUsuario.ADMIN)), efectos)
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
        job.cancel()
    }

    @Test
    fun `editar el correo o la contrasena limpia el error anterior`() = runTest {
        viewModel.onEmailChanged("correo-malo")
        viewModel.onPasswordChanged("Induztek2026!")
        viewModel.login()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.onEmailChanged("tecnico@induztek.cl")

        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `togglePasswordVisibility alterna la visibilidad`() {
        assertFalse(viewModel.uiState.value.isPasswordVisible)
        viewModel.togglePasswordVisibility()
        assertTrue(viewModel.uiState.value.isPasswordVisible)
    }
}
