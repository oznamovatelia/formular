package util;

import com.softip.uoo.server.exception.AESRuntimeException;
import com.softip.uoo.server.model.dto.AESEncryptDto;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;

@Slf4j
public class AESUtil {
    private AESUtil() {
    }

    static SecureRandom random = new SecureRandom();

    public static final int GCM_IV_LENGTH = 12;
    public static final int GCM_TAG_LENGTH = 16;

    public static Key readKey(InputStream isKey, String storePassword, String password, String alias) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");

            keyStore.load(isKey, storePassword.toCharArray());

            if (!keyStore.containsAlias(alias)) {
                throw new AESRuntimeException("Alias for key not found");
            }
            return keyStore.getKey(alias, password.toCharArray());
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            log.error(e.getMessage(), e);
            throw new AESRuntimeException(e);
        }
    }

    public static AESEncryptDto encrypt(Key key, byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        byte[] iv = new byte[GCM_IV_LENGTH];

        random.nextBytes(iv);

        // Get Cipher Instance
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        // Create SecretKeySpec
        SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), "AES");

        // Create GCMParameterSpec
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);

        // Initialize Cipher for ENCRYPT_MODE
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);

        // Perform Encryption
        byte[] cipherText = cipher.doFinal(data);

        return AESEncryptDto.builder().cipherText(cipherText).iv(iv).build();
    }

    public static String decrypt(Key key, byte[] iv, byte[] data) {
        try {
            // Get Cipher Instance
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            // Create SecretKeySpec
            SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), "AES");

            // Create GCMParameterSpec
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);

            // Initialize Cipher for DECRYPT_MODE
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);

            // Perform Decryption
            byte[] decryptedText = cipher.doFinal(data);

            return new String(decryptedText);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            log.error(e.getMessage(), e);
            throw new AESRuntimeException(e);

        }
    }
}
