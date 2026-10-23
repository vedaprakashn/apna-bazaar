package com.heyhood.provider;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class PrivateLink {
  private static final String ALIAS = "heyhood-provider-link";
  private static SecretKey key() throws Exception {
    KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
    if (!store.containsAlias(ALIAS)) {
      KeyGenerator gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
      gen.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
          .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
      return gen.generateKey();
    }
    return (SecretKey) store.getKey(ALIAS, null);
  }
  static void save(Context context, String value) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key());
    String encrypted = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + "." +
        Base64.encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
    if (!context.getSharedPreferences("provider", 0).edit().putString("entry", encrypted).commit()) throw new Exception("Storage unavailable");
  }
  static String read(Context context) throws Exception {
    String stored = context.getSharedPreferences("provider", 0).getString("entry", null);
    if (stored == null) return null;
    String[] parts = stored.split("\\.", 2); if (parts.length != 2) return null;
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
    return MainActivity.normalize(new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8));
  }
  static void clear(Context context) { context.getSharedPreferences("provider", 0).edit().clear().commit(); }
}
