package nu.fgv.register.server.util.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
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
@Converter
public class CryptoConverter implements AttributeConverter<String, String> {

    private final byte[] secretKey;
    private final IvParameterSpec iv;
    private final Cipher cipher;

    public CryptoConverter(
            @Value("${spexregister.crypto.algorithm}") final String algorithm,
            @Value("${spexregister.crypto.secret-key}") final String secretKey,
            @Value("${spexregister.crypto.initialization-vector}") final String iv) {
        this.secretKey = secretKey.getBytes(StandardCharsets.UTF_8);
        this.iv = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
        try {
            cipher = Cipher.getInstance(algorithm);
        } catch (Exception e) {
            log.error("Error during initialization", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized String convertToDatabaseColumn(final String plainValue) {
        if (hasText(plainValue)) {
            final Key key = new SecretKeySpec(secretKey, "AES");

            try {
                cipher.init(Cipher.ENCRYPT_MODE, key, iv);
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
    public synchronized String convertToEntityAttribute(final String encryptedValue) {
        if (hasText(encryptedValue)) {
            final Key key = new SecretKeySpec(secretKey, "AES");

            try {
                cipher.init(Cipher.DECRYPT_MODE, key, iv);
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
