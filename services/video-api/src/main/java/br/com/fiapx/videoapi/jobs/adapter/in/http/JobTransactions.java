package br.com.fiapx.videoapi.jobs.adapter.in.http;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
final class JobTransactions implements JobTransactionExecutor {
    private final TransactionTemplate transactions;

    JobTransactions(TransactionTemplate transactions) {
        this.transactions = transactions;
    }

    public <T> T execute(Supplier<T> operation) {
        return transactions.execute(status -> operation.get());
    }
}
