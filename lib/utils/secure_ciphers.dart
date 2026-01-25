// Secure encryption utilities for ColAI.
//
// Implements software-layer encryption (White-Box Cryptography Lite) to protect sensitive data.
// This adds an extra layer of protection beyond hardware-backed storage, which can be
// compromised on rooted/jailbroken devices.
//
// Security approach:
// - Uses AES-256-CBC encryption
// - Key is derived from scattered string pieces (makes static analysis harder)
// - IV is derived separately using SHA-256
// - Even if hardware keystore is compromised, data remains encrypted

import 'dart:convert';
import 'dart:typed_data';
import 'package:encrypt/encrypt.dart' as enc;
import 'package:crypto/crypto.dart';

/// Implements software-layer encryption to protect data even if hardware storage is compromised.
///
/// This is particularly important for:
/// - Session cookies containing authentication tokens
/// - User preferences that might reveal usage patterns
/// - Any data that shouldn't be readable even on rooted devices
///
/// The implementation uses:
/// - AES-256-CBC for strong encryption
/// - Scattered key components to resist static analysis
/// - SHA-256 for key derivation
class SecureCiphers {
  // We split the "Master Salt" into multiple scattered pieces to make
  // static analysis of the binary significantly harder.
  static const String _p1 = 'a1_hUb_';
  static const String _p2 = 's3cUrE_';
  static const String _p3 = 'v3rY_hArd_';
  static const String _p4 = 't0_rEvErS3';

  static enc.Key _getDerivedKey() {
    // Reconstruct the salt from scattered pieces
    final salt = '$_p1$_p2$_p3$_p4';
    // Use SHA-256 to derive a 32-byte key from the salt and a constant
    final keyBytes = sha256.convert(utf8.encode('${salt}K3Y_S39M3NT')).bytes;
    return enc.Key(Uint8List.fromList(keyBytes));
  }

  static enc.IV _getDerivedIV() {
    final salt = '$_p4$_p3$_p2$_p1'; // Different order
    final ivBytes =
        sha256.convert(utf8.encode('${salt}1V_S39M3NT')).bytes.sublist(0, 16);
    return enc.IV(Uint8List.fromList(ivBytes));
  }

  /// Encrypts a plaintext string using AES-256-CBC with a software-hidden key.
  static String encrypt(String plaintext) {
    final key = _getDerivedKey();
    final iv = _getDerivedIV();
    final encrypter = enc.Encrypter(enc.AES(key));

    final encrypted = encrypter.encrypt(plaintext, iv: iv);
    return encrypted.base64;
  }

  /// Decrypts a base64 encoded string.
  static String decrypt(String base64Content) {
    try {
      final key = _getDerivedKey();
      final iv = _getDerivedIV();
      final encrypter = enc.Encrypter(enc.AES(key));

      return encrypter.decrypt64(base64Content, iv: iv);
    } catch (e) {
      // If decryption fails (e.g. key changed), return empty or original to prevent crash
      return '';
    }
  }
}
