package secret;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Provides possibility to encrypt and decrypt text using AES 256 / CBC / PKCS5Padding
 * Reads AES key from PKCS12 keystore
 */
public class Encryptor {
  private KeyStore.SecretKeyEntry secretKeyEntry;

  public Encryptor(String keystoreLocation, String keystorePass) throws IOException, GeneralSecurityException {
    char[] keystorePassChars = keystorePass == null ? null : keystorePass.toCharArray();
    KeyStore store = KeyStore.getInstance("PKCS12");
    InputStream input = new FileInputStream(keystoreLocation);
    store.load(input, keystorePassChars);
    secretKeyEntry = (KeyStore.SecretKeyEntry) store.getEntry("ariba_key", new KeyStore.PasswordProtection(keystorePassChars));
  }

  /**
   * Encrypt value with AES 256 with random initial vector. Initial vector is prepended to encrypted value
   *
   * @param value
   * @return
   */
  public String encrypt(String value) {
    try {
      Cipher cipher = getCipher();
      byte[] iv = new byte[16];
      new SecureRandom().nextBytes(iv);//Random initial vector
      cipher.init(Cipher.ENCRYPT_MODE, secretKeyEntry.getSecretKey(), new IvParameterSpec(iv));
      byte[] encryptedValue = cipher.doFinal(value.getBytes());
      return Base64.getEncoder().encodeToString(concat(iv, encryptedValue));
    } catch (GeneralSecurityException e) {
      System.out.println("Failed to encrypt. " + e.getMessage());
    }
    return null;
  }

  /**
   * Decrypt value with AES 256
   *
   * @param value
   * @return
   */
  public String decrypt(String value) {
    try {
      Cipher cipher = getCipher();
      byte[] decodedValue = Base64.getDecoder().decode(value);
      byte[] iv = Arrays.copyOfRange(decodedValue, 0, 16);//Initial vector which is prepended to encrypted text
      byte[] encryptedValue = Arrays.copyOfRange(decodedValue, 16, decodedValue.length);
      cipher.init(Cipher.DECRYPT_MODE, secretKeyEntry.getSecretKey(), new IvParameterSpec(iv));
      return new String(cipher.doFinal(encryptedValue));
    } catch (GeneralSecurityException e) {
      System.out.println("Failed to decrypt. " + e.getMessage());
    }
    return null;
  }

  /**
   * Get cipher instance
   *
   * @return
   * @throws NoSuchAlgorithmException
   * @throws NoSuchPaddingException
   */
  private Cipher getCipher() throws NoSuchAlgorithmException, NoSuchPaddingException {
    return Cipher.getInstance("AES/CBC/PKCS5Padding");
  }

  /**
   * Merge two byte arrays
   *
   * @param b1 first byte array
   * @param b2 second byte array
   * @return
   */
  private static byte[] concat(byte[] b1, byte[] b2) {
    byte[] result = Arrays.copyOf(b1, b1.length + b2.length);
    System.arraycopy(b2, 0, result, b1.length, b2.length);
    return result;
  }
}