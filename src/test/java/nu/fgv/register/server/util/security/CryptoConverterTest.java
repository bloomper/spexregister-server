package nu.fgv.register.server.util.security;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class CryptoConverterTest {

    @Test
    void should_encrypt_and_decrypt() {
        final CryptoConverter converter = new CryptoConverter("AES/CFB/PKCS5Padding", "Zr4t7w!z%C*F-JaNdRgUkXp2s5v8x/A?", "2546540121759905");

        final String plainValue = "whatever";
        final String encryptedValue = converter.convertToDatabaseColumn(plainValue);
        final String decryptedValue = converter.convertToEntityAttribute(encryptedValue);

        assertThat(decryptedValue, is(equalTo(plainValue)));
    }

}