package io.scalechain.blockchain.oap.blockchain.scalechain;

import io.scalechain.blockchain.chain.Blockchain;
import io.scalechain.blockchain.oap.blockchain.IBlockchainInterface;
import io.scalechain.blockchain.proto.Hash;
import io.scalechain.blockchain.proto.OutPoint;
import io.scalechain.blockchain.proto.Transaction;
import io.scalechain.blockchain.proto.TransactionOutput;
import io.scalechain.blockchain.storage.index.KeyValueDatabase;
import scala.Option;

/**
 * Created by shannon on 16. 11. 21.
 */
public class ScalechainBlockchainInterface implements IBlockchainInterface {
    Blockchain chain;
    public ScalechainBlockchainInterface(Blockchain chain) {
        this.chain = chain;
    }

    @Override
    public Transaction getTransation(Hash txId) {
        Option<Transaction> transactionOption = chain.getTransaction(txId, chain.get().db());
        if (transactionOption.isDefined()) return transactionOption.get();
        else return null;
    }

    @Override
    public TransactionOutput getTransactionOutput(OutPoint outPoint) {
        return chain.getTransactionOutput(outPoint, Blockchain.get().db());
    }

    @Override
    public KeyValueDatabase db() {
        return Blockchain.get().db();
    }
}