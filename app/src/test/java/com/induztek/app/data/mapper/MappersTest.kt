package com.induztek.app.data.mapper

import com.induztek.app.data.local.entity.EquipoEntity
import com.induztek.app.data.local.entity.PruebaEntity
import com.induztek.app.domain.model.EstadoPrueba
import com.induztek.app.domain.model.TipoEquipo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersTest {

    private fun pruebaEntity(estado: String, mediciones: String = """{"resistencia":"2.3 Ω"}""") = PruebaEntity(
        id = 1, equipoId = 1, fecha = "2026-09-23", hora = "14:30", tecnico = "Carlos",
        medicionesJson = mediciones, estado = estado
    )

    @Test
    fun `estado conocido se mapea al enum correspondiente`() {
        assertEquals(EstadoPrueba.FORMALIZADO, pruebaEntity("FORMALIZADO").toDomain().estado)
    }

    @Test
    fun `estado desconocido no revienta y vuelve a registrado en terreno`() {
        assertEquals(EstadoPrueba.REGISTRADO_TERRENO, pruebaEntity("ESTADO_VIEJO").toDomain().estado)
    }

    @Test
    fun `mediciones se leen desde json y json corrupto da mapa vacio`() {
        assertEquals("2.3 Ω", pruebaEntity("FORMALIZADO").toDomain().mediciones["resistencia"])
        assertTrue(pruebaEntity("FORMALIZADO", mediciones = "{no es json").toDomain().mediciones.isEmpty())
    }

    @Test
    fun `ida y vuelta de prueba conserva estado y mediciones`() {
        val original = pruebaEntity("SINCRONIZADO").toDomain()

        val roundTrip = original.toEntity().toDomain()

        assertEquals(original, roundTrip)
    }

    @Test
    fun `tipo de equipo desconocido se mapea a OTRO`() {
        val entity = EquipoEntity(id = 1, codigo = "X-1", tipo = "TIPO_NUEVO", instalacion = "Planta")

        assertEquals(TipoEquipo.OTRO, entity.toDomain().tipo)
        assertEquals(TipoEquipo.RELE, EquipoEntity(2, "R-1", "RELE", "Planta").toDomain().tipo)
    }
}
