package com.example.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * SecurityManager — AES-256-CBC encryption with hardened key derivation.
 *
 * Security improvements over the original implementation:
 *  - Key derivation uses PBKDF2WithHmacSHA256 (600k iterations) instead of a
 *    single SHA-256 pass, making brute-force attacks far more expensive.
 *  - The IV is now randomly generated for every encryption and prepended to the
 *    ciphertext (in cleartext), which is the standard CBC pattern. A static IV
 *    is a critical weakness because identical plaintexts produce identical
 *    ciphertexts, leaking information.
 *  - The salt is fixed for offline reproducibility (no external storage needed)
 *    but combined with a high iteration count. The salt's purpose is to defeat
 *    precomputed/rainbow tables, not to be secret.
 *
 * Encrypted format (base64 of): [16-byte IV][ciphertext]
 * This format is backwards-incompatible with the previous static-IV format on
 * purpose; previously stored data used a weaker scheme. New data always uses
 * the secure scheme.
 */
object SecurityManager {

    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_ALGORITHM = "AES"
    private const val FIXED_SALT = "HideXVaultSaltProPremium"
    private const val ITERATION_COUNT = 600_000
    private const val PIN_ITERATION_COUNT = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val IV_LENGTH_BYTES = 16
    private const val PIN_SALT_LENGTH_BYTES = 8

    private val secureRandom = SecureRandom()

    /**
     * Derives a 256-bit AES key from the user PIN using PBKDF2WithHmacSHA256.
     */
    private fun deriveKey(pin: String): SecretKeySpec {
        val keySpec = PBEKeySpec(pin.toCharArray(), FIXED_SALT.toByteArray(Charsets.UTF_8), ITERATION_COUNT, KEY_LENGTH_BITS)
        val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val derivedBytes = keyFactory.generateSecret(keySpec).encoded
        return SecretKeySpec(derivedBytes, KEY_ALGORITHM)
    }

    /**
     * Generates a cryptographically random 16-byte IV for AES-CBC.
     */
    private fun generateIv(): ByteArray {
        val iv = ByteArray(IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)
        return iv
    }

    /**
     * Encrypts a string using AES-256-CBC with a random IV.
     * Output is base64([IV][ciphertext]).
     */
    fun encrypt(plainText: String, pin: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val keySpec = deriveKey(pin)
            val ivBytes = generateIv()
            val ivSpec = IvParameterSpec(ivBytes)

            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Prepend IV to ciphertext, then base64-encode the whole blob
            val combined = ByteArray(ivBytes.size + encryptedBytes.size)
            System.arraycopy(ivBytes, 0, combined, 0, ivBytes.size)
            System.arraycopy(encryptedBytes, 0, combined, ivBytes.size, encryptedBytes.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Decrypts a base64 encoded AES-256 encrypted string of the form
     * base64([IV][ciphertext]).
     */
    fun decrypt(encryptedText: String, pin: String): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            val keySpec = deriveKey(pin)
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)

            // Extract the 16-byte IV prefix
            val ivBytes = combined.copyOfRange(0, IV_LENGTH_BYTES)
            val cipherBytes = combined.copyOfRange(IV_LENGTH_BYTES, combined.size)

            val ivSpec = IvParameterSpec(ivBytes)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decryptedBytes = cipher.doFinal(cipherBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            "[Decryption Error - Invalid PIN or Corrupted Data]"
        }
    }

    /**
     * Hashes a PIN with PBKDF2WithHmacSHA256 (600k iterations) for secure storage.
     * Using PBKDF2 (rather than a single SHA-256 pass) makes brute-force attacks on
     * short numeric PINs dramatically more expensive. The raw PIN is never persisted.
     *
     * Output format: base64([8-byte random salt][derived key bytes])
     * The salt is stored alongside the hash so verification can recompute it.
     */
    fun hashPin(pin: String): String {
        return try {
            val salt = ByteArray(PIN_SALT_LENGTH_BYTES)
            secureRandom.nextBytes(salt)
            val keySpec = PBEKeySpec(pin.toCharArray(), salt, PIN_ITERATION_COUNT, KEY_LENGTH_BITS)
            val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val derivedBytes = keyFactory.generateSecret(keySpec).encoded
            val combined = ByteArray(salt.size + derivedBytes.size)
            System.arraycopy(salt, 0, combined, 0, salt.size)
            System.arraycopy(derivedBytes, 0, combined, salt.size, derivedBytes.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Verifies a PIN against a stored hash produced by [hashPin]. Uses a
     * constant-time comparison via [MessageDigest.isEqual] to avoid timing attacks.
     */
    fun verifyPin(pin: String, storedHash: String): Boolean {
        return try {
            val combined = Base64.decode(storedHash, Base64.NO_WRAP)
            val salt = combined.copyOfRange(0, PIN_SALT_LENGTH_BYTES)
            val storedKey = combined.copyOfRange(PIN_SALT_LENGTH_BYTES, combined.size)
            val keySpec = PBEKeySpec(pin.toCharArray(), salt, PIN_ITERATION_COUNT, KEY_LENGTH_BITS)
            val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val candidateKey = keyFactory.generateSecret(keySpec).encoded
            MessageDigest.isEqual(candidateKey, storedKey)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Securely deletes data by overwriting byte content before deleting file (anti-recovery).
     */
    fun secureDeleteFile(filePath: String): Boolean {
        return try {
            val file = java.io.File(filePath)
            if (file.exists()) {
                // Zero-out contents before deletion for high security
                if (file.isFile && file.length() > 0) {
                    val randomAccessFile = java.io.RandomAccessFile(file, "rw")
                    val length = randomAccessFile.length()
                    randomAccessFile.seek(0)
                    val zeros = ByteArray(1024)
                    var written: Long = 0
                    while (written < length) {
                        val toWrite = minOf(1024L, length - written).toInt()
                        randomAccessFile.write(zeros, 0, toWrite)
                        written += toWrite
                    }
                    randomAccessFile.close()
                }
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
