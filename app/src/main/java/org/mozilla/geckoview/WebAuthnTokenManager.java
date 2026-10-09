package org.mozilla.geckoview;

import java.nio.ByteBuffer;
import org.mozilla.gecko.util.GeckoBundle;

// Replaces GeckoView's Play Services based WebAuthn bridge.
// Passkeys and security keys are unavailable; pages see a failed WebAuthn call.
class WebAuthnTokenManager {
  WebAuthnTokenManager() {}

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static GeckoResult webAuthnMakeCredential(
      GeckoBundle a, ByteBuffer b, ByteBuffer c, Object[] d, ByteBuffer e,
      GeckoBundle f, GeckoBundle g, int[] h, ByteBuffer i, String j) {
    return GeckoResult.fromException(
        new UnsupportedOperationException("WebAuthn is not supported in this build"));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static GeckoResult webAuthnGetAssertion(
      ByteBuffer a, Object[] b, ByteBuffer c, GeckoBundle d, GeckoBundle e,
      ByteBuffer f, String g) {
    return GeckoResult.fromException(
        new UnsupportedOperationException("WebAuthn is not supported in this build"));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static GeckoResult webAuthnIsUserVerifyingPlatformAuthenticatorAvailable() {
    return GeckoResult.fromValue(Boolean.FALSE);
  }
}
