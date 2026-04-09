package com.ebikes.notifications.database.converters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import javax.crypto.KeyGenerator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EncryptedStringConverter")
class EncryptedStringConverterTest {

  private static EncryptedStringConverter converter;

  private EncryptedStringConverterTest() {}

  @BeforeAll
  static void setUp() throws Exception {
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(256);
    String base64Key = Base64.getEncoder().encodeToString(keyGen.generateKey().getEncoded());
    converter = new EncryptedStringConverter(base64Key);
  }

  @Nested
  @DisplayName("convertToDatabaseColumn")
  class ConvertToDatabaseColumn {

    @Test
    @DisplayName("should return null for null input")
    void shouldReturnNullForNull() {
      assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    @DisplayName("should return a non-blank Base64 string for plaintext input")
    void shouldReturnBase64EncodedCiphertext() {
      String result = converter.convertToDatabaseColumn("hello");

      assertThat(result).isNotBlank();
      // Must be valid Base64
      assertThat(Base64.getDecoder().decode(result)).isNotEmpty();
    }

    @Test
    @DisplayName("should produce a different ciphertext on each call due to random IV")
    void shouldProduceDifferentCiphertextEachCall() {
      String first = converter.convertToDatabaseColumn("same plaintext");
      String second = converter.convertToDatabaseColumn("same plaintext");

      assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("should throw IllegalStateException when key is invalid")
    void shouldThrowOnInvalidKey() {
      assertThatThrownBy(() -> new EncryptedStringConverter("not-valid-base64!!!"))
              .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("convertToEntityAttribute")
  class ConvertToEntityAttribute {

    @Test
    @DisplayName("should return null for null input")
    void shouldReturnNullForNull() {
      assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    @DisplayName("should throw IllegalStateException for tampered ciphertext")
    void shouldThrowForTamperedCiphertext() {
      String valid = converter.convertToDatabaseColumn("secret");
      byte[] payload = Base64.getDecoder().decode(valid);
      payload[payload.length - 1] ^= 0xFF; // flip bits in auth tag
      String tampered = Base64.getEncoder().encodeToString(payload);

      assertThatThrownBy(() -> converter.convertToEntityAttribute(tampered))
              .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("should throw IllegalStateException for arbitrary invalid input")
    void shouldThrowForInvalidInput() {
      assertThatThrownBy(() -> converter.convertToEntityAttribute("not-valid-base64!!!"))
              .isInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  @DisplayName("round-trip")
  class RoundTrip {

    @Test
    @DisplayName("should decrypt back to original plaintext")
    void shouldRoundTripPlaintext() {
      String plaintext = "sensitive@email.com";

      String encrypted = converter.convertToDatabaseColumn(plaintext);
      String decrypted = converter.convertToEntityAttribute(encrypted);

      assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("should round-trip an empty string")
    void shouldRoundTripEmptyString() {
      String encrypted = converter.convertToDatabaseColumn("");
      String decrypted = converter.convertToEntityAttribute(encrypted);

      assertThat(decrypted).isEmpty();
    }

    @Test
    @DisplayName("should round-trip a unicode string")
    void shouldRoundTripUnicode() {
      String plaintext = "Ünïcödé strïng 🔐";

      String encrypted = converter.convertToDatabaseColumn(plaintext);
      String decrypted = converter.convertToEntityAttribute(encrypted);

      assertThat(decrypted).isEqualTo(plaintext);
    }
  }
}