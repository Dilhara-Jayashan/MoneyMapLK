package com.example.moneymaplk.domain.validation

data class ValidationResult<out T>(
    val value: T? = null,
    val errorMessage: String? = null
) {
    val isValid: Boolean
        get() = errorMessage == null

    companion object {
        fun <T> success(value: T): ValidationResult<T> {
            return ValidationResult(value = value)
        }

        fun <T> error(message: String): ValidationResult<T> {
            return ValidationResult(errorMessage = message)
        }
    }
}
