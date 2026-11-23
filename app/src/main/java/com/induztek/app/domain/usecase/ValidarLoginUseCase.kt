package com.induztek.app.domain.usecase

import com.induztek.app.domain.model.Usuario
import com.induztek.app.domain.policy.PasswordPolicy
import com.induztek.app.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Caso de uso: Validación y ejecución del inicio de sesión.
 *
 * Aplica las reglas de negocio (PasswordPolicy y formato email)
 * antes de delegar al AuthRepository.
 */
class ValidarLoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val passwordPolicy: PasswordPolicy
) {
    suspend operator fun invoke(email: String, password: String): Result<Usuario> {
        val trimmedEmail = email.trim()

        if (trimmedEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("El correo electrónico es requerido"))
        }

        if (!EMAIL_REGEX.matches(trimmedEmail)) {
            return Result.failure(IllegalArgumentException("El formato del correo electrónico no es válido"))
        }

        // Evaluar políticas de seguridad sobre la contraseña
        when (val policyResult = passwordPolicy.validate(password)) {
            is PasswordPolicy.ValidationResult.Invalid -> {
                val errorMsg = policyResult.errors.firstOrNull() ?: "Contraseña no cumple requisitos de seguridad"
                return Result.failure(IllegalArgumentException(errorMsg))
            }
            is PasswordPolicy.ValidationResult.Valid -> {
                // Proceder con autenticación
                return authRepository.login(trimmedEmail, password)
            }
        }
    }

    private companion object {
        // Regex puro de Kotlin: android.util.Patterns no existe en tests JVM (NPE / "not mocked").
        val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")
    }
}
