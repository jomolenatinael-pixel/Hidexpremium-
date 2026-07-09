package com.example.security

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityManager {

    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val FIXED_SALT = "HideXVaultSaltProPremium" // Static salt to guarantee consistent key derivation

    /**
     * Derives a 256-bit AES key from the user PIN.
     */
    private fun deriveKey(pin: String): SecretKeySpec {
        val hashedBytes = MessageDigest.getInstance("SHA-256")
            .digest((pin + FIXED_SALT).toByteArray(Charsets.UTF_8))
        return SecretKeySpec(hashedBytes, "AES")
    }

    /**
     * Encrypts a string using AES-256 (CBC with an IV derived from the key).
     */
    fun encrypt(plainText: String, pin: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val keySpec = deriveKey(pin)
            val cipher = Cipher.getInstance(ALGORITHM)
            
            // Create static IV from first 16 bytes of SHA-256 of PIN to keep it offline-reproducible
            val ivBytes = MessageDigest.getInstance("SHA-256")
                .digest(pin.toByteArray(Charsets.UTF_8))
                .copyOf(16)
            val ivSpec = IvParameterSpec(ivBytes)

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Decrypts a base64 encoded AES-256 encrypted string.
     */
    fun decrypt(encryptedText: String, pin: String): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            val keySpec = deriveKey(pin)
            val cipher = Cipher.getInstance(ALGORITHM)

            val ivBytes = MessageDigest.getInstance("SHA-256")
                .digest(pin.toByteArray(Charsets.UTF_8))
                .copyOf(16)
            val ivSpec = IvParameterSpec(ivBytes)

            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decodedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            "[Decryption Error - Invalid PIN or Corrupted Data]"
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
