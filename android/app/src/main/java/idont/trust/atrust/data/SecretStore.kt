package idont.trust.atrust.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import idont.trust.atrust.logging.Logger

class SecretStore(context: Context) {
    private val preferences = context.getSharedPreferences("encrypted_secrets", Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    fun readPassword(): String = read(PASSWORD)
    fun readClientData(): String = read(CLIENT_DATA)
    fun readTotpSecret(): String = read(TOTP_SECRET)
    fun writePassword(password: String) = write(PASSWORD, password)
    fun writeClientData(clientData: String) = write(CLIENT_DATA, clientData)
    fun writeTotpSecret(secret: String) = write(TOTP_SECRET, secret)

    private fun read(name: String): String {
        val encoded = preferences.getString(name, null) ?: return ""
        return runCatching {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            val ivLength = payload.first().toInt() and 0xff
            val iv = payload.copyOfRange(1, 1 + ivLength)
            val encrypted = payload.copyOfRange(1 + ivLength, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            cipher.doFinal(encrypted).decodeToString()
        }.onFailure {
            Logger.e("SecretStore", "Failed to decrypt secret '$name'; returning empty value", it)
        }.getOrDefault("")
    }

    private fun write(name: String, value: String) {
        if (value.isEmpty()) {
            Logger.d("SecretStore", "Removing encrypted secret '$name'")
            preferences.edit().remove(name).apply()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.encodeToByteArray())
        val payload = byteArrayOf(cipher.iv.size.toByte()) + cipher.iv + encrypted
        preferences.edit()
            .putString(name, Base64.encodeToString(payload, Base64.NO_WRAP))
            .apply()
        Logger.d("SecretStore", "Encrypted secret '$name' updated")
    }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        Logger.i("SecretStore", "Generating Android Keystore key")
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "distrust-profile-secrets-v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PASSWORD = "default_profile_password"
        const val CLIENT_DATA = "default_profile_client_data"
        const val TOTP_SECRET = "default_profile_totp_secret"
    }
}
