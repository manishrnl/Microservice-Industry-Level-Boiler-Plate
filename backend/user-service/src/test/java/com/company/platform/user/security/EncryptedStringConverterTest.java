package com.company.platform.user.security;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

class EncryptedStringConverterTest {

    @Test
    void encryptsPlainTextAndDecryptsItBack() {
        EncryptedStringConverter converter = new EncryptedStringConverter();

        String encrypted = converter.convertToDatabaseColumn("Sensitive Value");
        String decrypted = converter.convertToEntityAttribute(encrypted);

        assertTrue(encrypted.startsWith("enc:v1:"));
        assertNotEquals(encrypted, "Sensitive Value");
        assertEquals(decrypted, "Sensitive Value");
    }

    @Test
    void keepsNullBlankAndAlreadyEncryptedValuesUntouched() {
        EncryptedStringConverter converter = new EncryptedStringConverter();

        assertNull(converter.convertToDatabaseColumn(null));
        assertEquals(converter.convertToDatabaseColumn(" "), " ");
        assertEquals(converter.convertToDatabaseColumn("enc:v1:abc"), "enc:v1:abc");
        assertEquals(converter.convertToEntityAttribute("plain"), "plain");
    }

    @Test
    void invalidEncryptedPayloadThrowsClearFailure() {
        EncryptedStringConverter converter = new EncryptedStringConverter();

        IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> converter.convertToEntityAttribute("enc:v1:not-base64")
        );
        assertTrue(exception.getMessage().contains("Unable to decrypt"));
    }
}
