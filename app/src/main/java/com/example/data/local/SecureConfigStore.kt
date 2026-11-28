package com.example.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("remainderr_secure_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ALIAS = "remainderr_sync_aes_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PREF_DRIVE_EMAIL = "enc_drive_email"
        private const val PREF_DRIVE_FOLDER_ID = "enc_drive_folder_id"
        private const val PREF_LAST_SYNCED_AT = "drive_last_synced_at"
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(payload: String): String {
        val parts = payload.split(":")
        if (parts.size != 2) return ""
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    fun getDriveEmail(): String {
        val stored = prefs.getString(PREF_DRIVE_EMAIL, null) ?: return ""
        return runCatching { decrypt(stored) }.getOrDefault("")
    }

    fun setDriveEmail(email: String) {
        if (email.isBlank()) {
            prefs.edit().remove(PREF_DRIVE_EMAIL).apply()
        } else {
            prefs.edit().putString(PREF_DRIVE_EMAIL, encrypt(email.trim())).apply()
        }
    }

    fun getDriveFolderId(): String {
        val stored = prefs.getString(PREF_DRIVE_FOLDER_ID, null) ?: return ""
        return runCatching { decrypt(stored) }.getOrDefault("")
    }

    fun setDriveFolderId(folderId: String) {
        if (folderId.isBlank()) {
            prefs.edit().remove(PREF_DRIVE_FOLDER_ID).apply()
        } else {
            prefs.edit().putString(PREF_DRIVE_FOLDER_ID, encrypt(folderId.trim())).apply()
        }
    }

    fun getLastSyncedAt(): Long = prefs.getLong(PREF_LAST_SYNCED_AT, 0L)

    fun setLastSyncedAt(epochMillis: Long) {
        if (epochMillis <= 0L) {
            prefs.edit().remove(PREF_LAST_SYNCED_AT).apply()
        } else {
            prefs.edit().putLong(PREF_LAST_SYNCED_AT, epochMillis).apply()
        }
    }

    fun clearDriveSyncMetadata() {
        prefs.edit()
            .remove(PREF_DRIVE_EMAIL)
            .remove(PREF_DRIVE_FOLDER_ID)
            .remove(PREF_LAST_SYNCED_AT)
            .apply()
    }
}
