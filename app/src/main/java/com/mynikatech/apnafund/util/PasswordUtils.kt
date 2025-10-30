package com.mynikatech.apnafund.util

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordUtils {
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA1"

    fun hashPassword(password: String, salt: ByteArray = generateSalt()): String {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hash = factory.generateSecret(keySpec).encoded
        return "${Base64.encodeToString(salt, Base64.NO_WRAP)}:${
            Base64.encodeToString(
                hash,
                Base64.NO_WRAP
            )
        }"
    }

    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }
}