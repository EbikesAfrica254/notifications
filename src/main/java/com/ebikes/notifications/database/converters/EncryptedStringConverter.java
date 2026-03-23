package com.ebikes.notifications.database.converters;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Converter
@Slf4j
public class EncryptedStringConverter implements AttributeConverter<String, String> {

  private static final int GCM_AUTH_TAG_LENGTH = 128;
  private static final int GCM_IV_LENGTH = 12;
  private static final String ALGORITHM = "AES";
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";

  private final SecretKey secretKey;
  private final SecureRandom secureRandom;

  public EncryptedStringConverter(@Value("${encryption.key}") String base64Key) {
    byte[] keyBytes = Base64.getDecoder().decode(base64Key);
    this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
    this.secureRandom = new SecureRandom();
  }

  @Override
  public String convertToDatabaseColumn(String plaintext) {
    if (plaintext == null) {
      return null;
    }
    try {
      byte[] iv = new byte[GCM_IV_LENGTH];
      secureRandom.nextBytes(iv);

      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_AUTH_TAG_LENGTH, iv));

      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      byte[] payload = new byte[GCM_IV_LENGTH + ciphertext.length];

      System.arraycopy(iv, 0, payload, 0, GCM_IV_LENGTH);
      System.arraycopy(ciphertext, 0, payload, GCM_IV_LENGTH, ciphertext.length);

      return Base64.getEncoder().encodeToString(payload);
    } catch (Exception e) {
      log.error("Failed to encrypt column value", e);
      throw new IllegalStateException("Column encryption failed", e);
    }
  }

  @Override
  public String convertToEntityAttribute(String encoded) {
    if (encoded == null) {
      return null;
    }
    try {
      byte[] payload = Base64.getDecoder().decode(encoded);
      byte[] iv = new byte[GCM_IV_LENGTH];
      byte[] ciphertext = new byte[payload.length - GCM_IV_LENGTH];

      System.arraycopy(payload, 0, iv, 0, GCM_IV_LENGTH);
      System.arraycopy(payload, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_AUTH_TAG_LENGTH, iv));

      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    } catch (Exception e) {
      log.error("Failed to decrypt column value", e);
      throw new IllegalStateException("Column decryption failed", e);
    }
  }
}
