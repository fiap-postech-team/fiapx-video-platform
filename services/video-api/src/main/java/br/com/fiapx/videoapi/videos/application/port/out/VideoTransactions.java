package br.com.fiapx.videoapi.videos.application.port.out;

import java.util.function.Supplier;

public interface VideoTransactions {
    <T> T execute(Supplier<T> operation);
}
