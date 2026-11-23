package com.induztek.app.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.induztek.app.data.local.entity.PruebaEntity
import com.induztek.app.domain.model.EstadoPrueba
import com.induztek.app.domain.model.Prueba

private val gson = Gson()
private val mapType = object : TypeToken<Map<String, String>>() {}.type

fun PruebaEntity.toDomain(): Prueba = Prueba(
    id             = id,
    equipoId       = equipoId,
    fecha          = fecha,
    hora           = hora,
    tecnico        = tecnico,
    mediciones     = runCatching { gson.fromJson<Map<String, String>>(medicionesJson, mapType) }
        .getOrNull() ?: emptyMap(),
    observaciones  = observaciones,
    fotoUriPath    = fotoUriPath,
    // Un estado desconocido en la base no debe tumbar la app: se trata como registrado en terreno.
    estado         = EstadoPrueba.values().firstOrNull { it.name == estado } ?: EstadoPrueba.REGISTRADO_TERRENO,
    sincronizadoAt = sincronizadoAt
)

fun Prueba.toEntity(): PruebaEntity = PruebaEntity(
    id             = id,
    equipoId       = equipoId,
    fecha          = fecha,
    hora           = hora,
    tecnico        = tecnico,
    medicionesJson = gson.toJson(mediciones),
    observaciones  = observaciones,
    fotoUriPath    = fotoUriPath,
    estado         = estado.name,
    sincronizadoAt = sincronizadoAt
)
