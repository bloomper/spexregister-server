package nu.fgv.register.server.util;

import jakarta.persistence.AttributeConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
public class CryptoConverter implements AttributeConverter<String, String> {

    private final byte[] secretKey;
    private final Cipher cipher;

    public CryptoConverter(
            @Value("${spexregister.encryption.algorithm}") final String algorithm,
            @Value("${spexregister.encryption.secret-key}") final String secretKey) {
        this.secretKey = secretKey.getBytes(StandardCharsets.UTF_8);
        try {
            cipher = Cipher.getInstance(algorithm);
        } catch (Exception e) {
            log.error("Error during initialization", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String convertToDatabaseColumn(final String plainValue) {
        if (hasText(plainValue)) {
            final Key key = new SecretKeySpec(secretKey, "AES");

            try {
                cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(new byte[16]));
                return Base64.getEncoder().encodeToString(cipher.doFinal(plainValue.getBytes()));
            } catch (Exception e) {
                log.error("Unexpected error during encryption", e);
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    @Override
    public String convertToEntityAttribute(final String encryptedValue) {
        if (hasText(encryptedValue)) {
            final Key key = new SecretKeySpec(secretKey, "AES");

            try {
                cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(new byte[16]));
                return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedValue)));
            } catch (Exception e) {
                log.error("Unexpected error during decryption", e);
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }
}
