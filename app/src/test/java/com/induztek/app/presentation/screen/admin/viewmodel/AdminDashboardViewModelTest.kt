package com.induztek.app.presentation.screen.admin.viewmodel

import com.induztek.app.domain.model.EstadoPrueba
import com.induztek.app.domain.model.Prueba
import com.induztek.app.domain.model.RolUsuario
import com.induztek.app.domain.model.Usuario
import com.induztek.app.domain.repository.AuthRepository
import com.induztek.app.presentation.screen.nuevaprueba.viewmodel.NuevaPruebaViewModelTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminDashboardViewModelTest {

    private lateinit var pruebas: NuevaPruebaViewModelTest.FakePruebaRepository
    private lateinit var auth: FakeAuth

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        pruebas = NuevaPruebaViewModelTest.FakePruebaRepository()
        auth = FakeAuth()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = AdminDashboardViewModel(pruebas, auth)

    private fun prueba(id: Long, estado: EstadoPrueba) = Prueba(
        id = id, equipoId = 1, fecha = "2026-09-23", hora = "10:00", tecnico = "Carlos",
        mediciones = emptyMap(), estado = estado
    )

    private fun usuario(rol: RolUsuario) = Usuario(1, "x@induztek.cl", "X", rol, "token")

    @Test
    fun `un administrador puede formalizar una prueba registrada en terreno`() {
        pruebas.pruebas.add(prueba(1, EstadoPrueba.REGISTRADO_TERRENO))
        auth.usuario = usuario(RolUsuario.ADMIN)
        val vm = viewModel()

        vm.formalizarPrueba(1)

        assertEquals(EstadoPrueba.FORMALIZADO, pruebas.pruebas.single().estado)
        assertNotNull(vm.uiState.value.mensajeExito)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `un tecnico no puede formalizar aunque llame al viewmodel`() {
        pruebas.pruebas.add(prueba(1, EstadoPrueba.REGISTRADO_TERRENO))
        auth.usuario = usuario(RolUsuario.TECNICO)
        val vm = viewModel()

        vm.formalizarPrueba(1)

        assertEquals(EstadoPrueba.REGISTRADO_TERRENO, pruebas.pruebas.single().estado)
        assertNotNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.mensajeExito)
    }

    @Test
    fun `sin sesion tampoco se puede formalizar`() {
        pruebas.pruebas.add(prueba(1, EstadoPrueba.REGISTRADO_TERRENO))
        auth.usuario = null
        val vm = viewModel()

        vm.formalizarPrueba(1)

        assertEquals(EstadoPrueba.REGISTRADO_TERRENO, pruebas.pruebas.single().estado)
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `no se puede formalizar dos veces ni una prueba inexistente`() {
        pruebas.pruebas.add(prueba(1, EstadoPrueba.FORMALIZADO))
        auth.usuario = usuario(RolUsuario.ADMIN)
        val vm = viewModel()

        vm.formalizarPrueba(1)
        assertNotNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.mensajeExito)

        vm.limpiarMensajes()
        vm.formalizarPrueba(99)
        assertNotNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.mensajeExito)
    }

    private class FakeAuth : AuthRepository {
        var usuario: Usuario? = null
        private val flow = MutableStateFlow<Usuario?>(null)
        override val currentUsuario: StateFlow<Usuario?> = flow
        override suspend fun login(email: String, password: String): Result<Usuario> =
            Result.failure(UnsupportedOperationException())
        override suspend fun logout() {}
        override suspend fun getUsuarioActual(): Usuario? = usuario
    }
}
