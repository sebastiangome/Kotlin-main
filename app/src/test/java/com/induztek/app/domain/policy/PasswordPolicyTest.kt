package com.induztek.app.domain.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias de las 5 Reglas de Negocio en PasswordPolicy (Capa Domain).
 */
class PasswordPolicyTest {

    private lateinit var policy: PasswordPolicy

    @Before
    fun setUp() {
        policy = PasswordPolicy()
    }

    @Test
    fun `contrasena valida con todas las reglas retorna Valid`() {
        val resultado = policy.validate("Induztek2026!")
        assertTrue(resultado is PasswordPolicy.ValidationResult.Valid)
    }

    @Test
    fun `contrasena corta menor a 8 caracteres retorna Invalid`() {
        val resultado = policy.validate("Ind1!")
        assertTrue(resultado is PasswordPolicy.ValidationResult.Invalid)
        val errors = (resultado as PasswordPolicy.ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("8 caracteres") })
    }

    @Test
    fun `contrasena sin mayuscula retorna Invalid`() {
        val resultado = policy.validate("induztek2026!")
        assertTrue(resultado is PasswordPolicy.ValidationResult.Invalid)
        val errors = (resultado as PasswordPolicy.ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("mayúscula") })
    }

    @Test
    fun `contrasena sin digito numerico retorna Invalid`() {
        val resultado = policy.validate("InduztekPass!")
        assertTrue(resultado is PasswordPolicy.ValidationResult.Invalid)
        val errors = (resultado as PasswordPolicy.ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("número") })
    }

    @Test
    fun `contrasena sin caracter especial retorna Invalid`() {
        val resultado = policy.validate("Induztek2026")
        assertTrue(resultado is PasswordPolicy.ValidationResult.Invalid)
        val errors = (resultado as PasswordPolicy.ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("carácter especial") })
    }

    @Test
    fun `contrasena sin minuscula retorna Invalid`() {
        val resultado = policy.validate("INDUZTEK2026!")
        assertTrue(resultado is PasswordPolicy.ValidationResult.Invalid)
        val errors = (resultado as PasswordPolicy.ValidationResult.Invalid).errors
        assertEquals(1, errors.size)
        assertTrue(errors.single().contains("minúscula"))
    }

    @Test
    fun `contrasena vacia incumple las 5 reglas`() {
        val resultado = policy.validate("")
        assertTrue(resultado is PasswordPolicy.ValidationResult.Invalid)
        assertEquals(5, (resultado as PasswordPolicy.ValidationResult.Invalid).errors.size)
    }

    @Test
    fun `contrasena valida con exactamente 8 caracteres es aceptada`() {
        assertTrue(policy.validate("Abcdef1!") is PasswordPolicy.ValidationResult.Valid)
    }

    @Test
    fun `contrasenas con una sola regla incumplida reportan solo ese error`() {
        listOf("induztek2026!", "InduztekPass!", "Induztek2026").forEach { clave ->
            val resultado = policy.validate(clave) as PasswordPolicy.ValidationResult.Invalid
            assertEquals("Debe fallar solo una regla: $clave", 1, resultado.errors.size)
        }
    }
}
