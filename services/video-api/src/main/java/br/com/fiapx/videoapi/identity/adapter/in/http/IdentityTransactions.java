package br.com.fiapx.videoapi.identity.adapter.in.http;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
final class IdentityTransactions {
    private final TransactionTemplate transactions;

    IdentityTransactions(TransactionTemplate transactions) {
        this.transactions = transactions;
    }

    <T> T execute(Supplier<T> operation) {
        return transactions.execute(status -> operation.get());
    }

    void execute(Runnable operation) {
        transactions.executeWithoutResult(status -> operation.run());
    }
}
