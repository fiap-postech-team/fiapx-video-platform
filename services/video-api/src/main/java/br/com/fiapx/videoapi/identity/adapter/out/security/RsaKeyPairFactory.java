package br.com.fiapx.videoapi.identity.adapter.out.security;

import br.com.fiapx.videoapi.identity.adapter.configuration.AuthProperties;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public final class RsaKeyPairFactory {
    private final AuthProperties properties;

    public RsaKeyPairFactory(AuthProperties properties) {
        this.properties = properties;
    }

    public KeyPair create() {
        if (!StringUtils.hasText(properties.privateKeyBase64()) || !StringUtils.hasText(properties.publicKeyBase64())) {
            return generated();
        }
        return decoded();
    }

    private KeyPair generated() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Cannot generate local RSA key pair", exception);
        }
    }

    private KeyPair decoded() {
        try {
            var factory = KeyFactory.getInstance("RSA");
            var privateKey = (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(decode(properties.privateKeyBase64())));
            var publicKey = (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(decode(properties.publicKeyBase64())));
            return new KeyPair(publicKey, privateKey);
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Invalid RSA key configuration", exception);
        }
    }

    private byte[] decode(String value) {
        return Base64.getDecoder().decode(value);
    }
}
