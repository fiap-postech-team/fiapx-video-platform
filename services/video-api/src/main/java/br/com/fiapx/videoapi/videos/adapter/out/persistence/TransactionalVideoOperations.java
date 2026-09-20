package br.com.fiapx.videoapi.videos.adapter.out.persistence;

import br.com.fiapx.videoapi.videos.application.port.out.VideoTransactions;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public final class TransactionalVideoOperations implements VideoTransactions {
    private final TransactionTemplate transactions;

    public TransactionalVideoOperations(TransactionTemplate transactions) {
        this.transactions = transactions;
    }

    public <T> T execute(Supplier<T> operation) {
        return transactions.execute(status -> operation.get());
    }
}
