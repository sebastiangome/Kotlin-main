package com.induztek.app.presentation.screen.nuevaprueba.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.induztek.app.domain.model.Equipo
import com.induztek.app.domain.model.EstadoPrueba
import com.induztek.app.domain.model.Prueba
import com.induztek.app.domain.model.TipoEquipo
import com.induztek.app.domain.repository.EquipoRepository
import com.induztek.app.domain.repository.PruebaRepository
import com.induztek.app.presentation.navigation.AppDestination
import com.induztek.app.presentation.screen.nuevaprueba.state.NuevaPruebaEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NuevaPruebaViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: NuevaPruebaViewModel
    private lateinit var fakePruebaRepo: FakePruebaRepository
    private lateinit var fakeEquipoRepo: FakeEquipoRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePruebaRepo = FakePruebaRepository()
        fakeEquipoRepo = FakeEquipoRepository()

        val savedStateHandle = SavedStateHandle(mapOf(AppDestination.NuevaPrueba.ARG_EQUIPO_ID to 1L))
        viewModel = NuevaPruebaViewModel(fakePruebaRepo, fakeEquipoRepo, savedStateHandle)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `estado inicial tiene fecha y hora pre-poblados`() {
        val state = viewModel.uiState.value
        assertTrue("fecha debe tener formato YYYY-MM-DD", state.fecha.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        assertTrue("hora debe tener formato HH:mm", state.hora.matches(Regex("\\d{2}:\\d{2}")))
        assertEquals(1L, state.equipoId)
    }

    @Test
    fun `guardarPrueba sin mediciones muestra error y no guarda`() {
        viewModel.onTecnicoChanged("Ing. Carlos Mendoza")
        viewModel.guardarPrueba()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(fakePruebaRepo.pruebas.isEmpty())
    }

    @Test
    fun `guardarPrueba exitoso emite NavigateToDetalle y persiste en estado REGISTRADO_TERRENO`() = runTest {
        viewModel.onTecnicoChanged("Ing. Carlos Mendoza")
        viewModel.onMedicionChanged("resistencia", "1250 MΩ")

        // El SharedFlow no tiene replay: el colector debe estar activo ANTES de guardar.
        // Con el dispatcher por defecto de runTest el colector arrancaria tarde y el efecto se perderia.
        val efectos = mutableListOf<NuevaPruebaEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { efectos.add(it) }
        }

        viewModel.guardarPrueba()
        advanceUntilIdle()

        assertEquals(1, efectos.size)
        assertTrue(efectos.first() is NuevaPruebaEffect.NavigateToDetalle)
        assertEquals(EstadoPrueba.REGISTRADO_TERRENO, fakePruebaRepo.pruebas.first().estado)
        assertTrue(viewModel.uiState.value.isSaved)
        job.cancel()
    }

    @Test
    fun `guardarPrueba sin tecnico muestra error y no guarda`() {
        viewModel.onMedicionChanged("resistencia_aislamiento", "1250")
        viewModel.guardarPrueba()

        assertEquals("Ingresa el nombre del técnico responsable", viewModel.uiState.value.errorMessage)
        assertTrue(fakePruebaRepo.pruebas.isEmpty())
    }

    @Test
    fun `guardarPrueba sin fecha o sin hora muestra error y no guarda`() {
        viewModel.onTecnicoChanged("Ing. Carlos Mendoza")
        viewModel.onMedicionChanged("temperatura", "25")

        viewModel.onFechaChanged("")
        viewModel.guardarPrueba()
        assertEquals("La fecha es requerida", viewModel.uiState.value.errorMessage)

        viewModel.onFechaChanged("2026-11-23")
        viewModel.onHoraChanged("")
        viewModel.guardarPrueba()
        assertEquals("La hora es requerida", viewModel.uiState.value.errorMessage)
        assertTrue(fakePruebaRepo.pruebas.isEmpty())
    }

    @Test
    fun `guardarPrueba agrega unidades tecnicas a las mediciones sin duplicarlas`() = runTest {
        viewModel.onTecnicoChanged("Ing. Carlos Mendoza")
        viewModel.onMedicionChanged("resistencia_aislamiento", "1250")
        viewModel.onMedicionChanged("tension_prueba", "5 kV")
        viewModel.onMedicionChanged("temperatura", "25")
        viewModel.onMedicionChanged("medicion_desconocida", "7")

        viewModel.guardarPrueba()
        advanceUntilIdle()

        val mediciones = fakePruebaRepo.pruebas.single().mediciones
        assertEquals("1250 MΩ", mediciones["resistencia_aislamiento"])
        assertEquals("5 kV", mediciones["tension_prueba"])
        assertEquals("25 °C", mediciones["temperatura"])
        assertEquals("7", mediciones["medicion_desconocida"])
    }

    @Test
    fun `guardarPrueba informa el error si falla la persistencia`() = runTest {
        fakePruebaRepo.failOnInsert = true
        viewModel.onTecnicoChanged("Ing. Carlos Mendoza")
        viewModel.onMedicionChanged("temperatura", "25")

        viewModel.guardarPrueba()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isSaved)
        assertTrue(state.errorMessage!!.startsWith("Error al guardar en Room"))
    }

    @Test
    fun `onFotoCaptured actualiza fotoUriPath en el estado`() {
        val rutaFoto = "/data/data/com.induztek.app/files/images/foto_evidencia.jpg"
        viewModel.onFotoCaptured(rutaFoto)
        assertEquals(rutaFoto, viewModel.uiState.value.fotoUriPath)
    }

    // ── Fakes ─────────────────────────────────────────────────────────────

    class FakePruebaRepository : PruebaRepository {
        val pruebas = mutableListOf<Prueba>()
        var failOnInsert = false
        private var nextId = 1L

        override fun getAll(): Flow<List<Prueba>> = emptyFlow()
        override fun getByEquipo(equipoId: Long): Flow<List<Prueba>> = emptyFlow()
        override fun getPendienteSync(): Flow<List<Prueba>> = emptyFlow()
        override suspend fun getById(id: Long): Prueba? = pruebas.find { it.id == id }

        override suspend fun insert(prueba: Prueba): Long {
            if (failOnInsert) throw IllegalStateException("fallo simulado")
            val id = nextId++
            pruebas.add(prueba.copy(id = id))
            return id
        }

        override suspend fun update(prueba: Prueba) {
            val idx = pruebas.indexOfFirst { it.id == prueba.id }
            if (idx >= 0) pruebas[idx] = prueba
        }

        override suspend fun updateEstado(id: Long, nuevoEstado: EstadoPrueba) {
            val idx = pruebas.indexOfFirst { it.id == id }
            if (idx >= 0) pruebas[idx] = pruebas[idx].copy(estado = nuevoEstado)
        }

        override suspend fun delete(prueba: Prueba) {
            pruebas.removeIf { it.id == prueba.id }
        }
    }

    class FakeEquipoRepository : EquipoRepository {
        override fun getAll(): Flow<List<Equipo>> = emptyFlow()
        override suspend fun getById(id: Long): Equipo = Equipo(
            id = id,
            codigo = "TRF-001",
            tipo = TipoEquipo.TRANSFORMADOR,
            instalacion = "Subestación Norte"
        )
        override suspend fun insert(equipo: Equipo): Long = 1L
        override suspend fun update(equipo: Equipo) {}
        override suspend fun delete(equipo: Equipo) {}
    }
}
