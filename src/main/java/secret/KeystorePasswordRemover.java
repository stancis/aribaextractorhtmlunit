package secret;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Enumeration;

/**
 * Creates copy of existing keystore, removes password
 */
public class KeystorePasswordRemover {
  public static void main(String[] args) throws GeneralSecurityException, IOException {
    if (args.length < 3) {
      System.out.println("Please provide operation keystore location, keystore password, new keystore name");
      System.out.println("For example: java -jar file.jar keystore.p12 pass keystore_nopass.p12");
      return;
    }

    String keystoreLocation = args[0];
    String keystorePass = args[1];
    try (InputStream currentKeystoreInputStream = new FileInputStream(keystoreLocation);
         FileOutputStream newKeystoreOutputStream = new FileOutputStream(args.length > 2 ? args[2] : "keystore_nopass.p12");) {
      // Creating an empty PKCS12 keystore
      KeyStore newKeystore = KeyStore.getInstance("PKCS12");
      newKeystore.load(null, null);

      //Open current keystore
      char[] keystorePassChars = keystorePass == null ? null : keystorePass.toCharArray();
      KeyStore keystore = KeyStore.getInstance("PKCS12");
      keystore.load(currentKeystoreInputStream, keystorePassChars);

      //Copy keys to new keystore
      Enumeration<String> aliases = keystore.aliases();
      KeyStore.PasswordProtection pass = new KeyStore.PasswordProtection(keystorePassChars);
      while (aliases.hasMoreElements()) {
        String alias = aliases.nextElement();
        newKeystore.setEntry(alias, keystore.getEntry(alias, pass), new KeyStore.PasswordProtection(new char[0]));
      }

      //Save keystore
      newKeystore.store(newKeystoreOutputStream, new char[0]);
    }
  }
}
