package br.com.fiapx.videoapi.identity.application.port.out;

public interface RefreshTokenGenerator {
    String generate();
    String hash(String token);
}
