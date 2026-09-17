package br.com.fiapx.videoapi.jobs.adapter.in.http;

import java.util.function.Supplier;

@FunctionalInterface
interface JobTransactionExecutor {
    <T> T execute(Supplier<T> operation);
}
