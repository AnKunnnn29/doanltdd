// KeyStoreManager.java (Bạn cần tự tạo file này)
package com.example.doan.Utils;
import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;

import java.io.IOException;
import java.security.*;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class KeyStoreManager {

    private static final String KEY_ALIAS = "biometric_token_key";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String CIPHER_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES + "/"
            + KeyProperties.BLOCK_MODE_CBC + "/"
            + KeyProperties.ENCRYPTION_PADDING_PKCS7;

    private static final String PREFS_NAME = "BiometricPrefs";
    private static final String ENCRYPTED_TOKEN_KEY = "encrypted_token";
    private static final String IV_KEY = "iv";

    private static SharedPreferences getBiometricPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Tạo SecretKey trong Android KeyStore
     */
    private static SecretKey getOrCreateSecretKey() throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER);

            KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setUserAuthenticationRequired(true)
                    .setInvalidatedByBiometricEnrollment(true)
                    .build();

            keyGenerator.init(keyGenParameterSpec);
            return keyGenerator.generateKey();
        }
        return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
    }

    /**
     * Khởi tạo Cipher để mã hóa (cho lần kích hoạt)
     */
    @NonNull
    public static BiometricPrompt.CryptoObject getEncryptionCryptoObject() throws GeneralSecurityException, IOException {
        SecretKey secretKey = getOrCreateSecretKey();
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return new BiometricPrompt.CryptoObject(cipher);
    }

    /**
     * Lưu Refresh Token đã được mã hóa sau khi xác thực thành công
     */
    public static void saveEncryptedToken(Context context, String token, @NonNull BiometricPrompt.CryptoObject cryptoObject) throws Exception {
        Cipher cipher = cryptoObject.getCipher();
        if (cipher == null) throw new GeneralSecurityException("Cipher is null");

        byte[] encryptedToken = cipher.doFinal(token.getBytes("UTF-8"));
        byte[] iv = cipher.getIV();

        SharedPreferences.Editor editor = getBiometricPrefs(context).edit();
        editor.putString(ENCRYPTED_TOKEN_KEY, Base64.encodeToString(encryptedToken, Base64.DEFAULT));
        editor.putString(IV_KEY, Base64.encodeToString(iv, Base64.DEFAULT));
        editor.apply();
    }

    /**
     * Khởi tạo Cipher để giải mã (cho lần đăng nhập)
     */
    @NonNull
    public static BiometricPrompt.CryptoObject getDecryptionCryptoObject(Context context) throws GeneralSecurityException, IOException, InvalidAlgorithmParameterException {
        SharedPreferences prefs = getBiometricPrefs(context);
        String base64Iv = prefs.getString(IV_KEY, null);
        String base64EncryptedToken = prefs.getString(ENCRYPTED_TOKEN_KEY, null);

        if (base64Iv == null || base64EncryptedToken == null) {
            throw new GeneralSecurityException("Encrypted data not found");
        }

        byte[] iv = Base64.decode(base64Iv, Base64.DEFAULT);

        SecretKey secretKey = getOrCreateSecretKey();
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);

        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        return new BiometricPrompt.CryptoObject(cipher);
    }

    /**
     * Giải mã Token sau khi xác thực thành công
     */
    public static String decryptToken(Context context, @NonNull BiometricPrompt.CryptoObject cryptoObject) throws Exception {
        Cipher cipher = cryptoObject.getCipher();
        if (cipher == null) throw new GeneralSecurityException("Cipher is null");

        SharedPreferences prefs = getBiometricPrefs(context);
        String base64EncryptedToken = prefs.getString(ENCRYPTED_TOKEN_KEY, null);
        if (base64EncryptedToken == null) {
            throw new GeneralSecurityException("Encrypted token not found");
        }
        byte[] encryptedToken = Base64.decode(base64EncryptedToken, Base64.DEFAULT);

        byte[] decryptedBytes = cipher.doFinal(encryptedToken);
        return new String(decryptedBytes, "UTF-8");
    }

    public static boolean isBiometricEnrolled(Context context) {
        SharedPreferences prefs = getBiometricPrefs(context);
        return prefs.contains(ENCRYPTED_TOKEN_KEY) && prefs.contains(IV_KEY);
    }

    public static void clearBiometricData(Context context) {
        getBiometricPrefs(context).edit().clear().apply();
    }
}
