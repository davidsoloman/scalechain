package io.scalechain.blockchain.chain.processor

import io.scalechain.blockchain.{ErrorCode, ChainException}
import io.scalechain.blockchain.chain.Blockchain
import io.scalechain.blockchain.proto.{Transaction, Hash}
import org.slf4j.LoggerFactory

import scala.collection.mutable.{ArrayBuffer, ListBuffer}

object TransactionProcessor extends TransactionProcessor(Blockchain.get)

/** Processes a received transaction.
  *
  */
class TransactionProcessor(val chain : Blockchain) {
  private val logger = LoggerFactory.getLogger(classOf[TransactionProcessor])

  /** See if a transaction exists. Checks orphan transactions as well.
    * naming rule : 'exists' checks orphan transactions as well, whereas hasNonOrphan does not.
    *
    * @param txHash The hash of the transaction to check the existence.
    * @return true if the transaction was found; None otherwise.
    */
  def exists(txHash : Hash) : Boolean = {
    chain.hasTransaction(txHash) || chain.txOrphange.hasOrphan(txHash)
  }

  /** Get a transaction either from a block or from the transaction disk-pool.
    * getTransaction does not return orphan transactions.
    *
    * @param txHash The hash of the transaction to get.
    * @return Some(transaction) if the transaction was found; None otherwise.
    */
  def getTransaction(txHash : Hash) : Option[Transaction] = {
    chain.getTransaction(txHash)
  }

  /**
    * Add a transaction to disk pool.
    *
    * Assumption : The transaction was pointing to a transaction record location, which points to a transaction written while the block was put into disk.
    *
    * @param txHash The hash of the transaction to add.
    * @param transaction The transaction to add to the disk-pool.
    * @return true if the transaction was valid with all inputs connected. false otherwise. (ex> orphan transactions return false )
    */
  def addTransactionToPool(txHash : Hash, transaction : Transaction) : Unit = {
    // TODO : Need to check if the validity of the transation?
    chain.txPool.addTransactionToPool(txHash, transaction)
  }

  /**
    * Recursively accepts children of the given parent.
    *
    * @param initialParentTxHash The hash of the parent transaction that an orphan might depend on.
    * @return The list of hashes of accepted children transactions.
    */
  def acceptChildren(initialParentTxHash : Hash) : List[Hash] = {
    val acceptedChildren = new ArrayBuffer[Hash]

    var i = -1;
    do {
      val parentTxHash = if (acceptedChildren.length == 0) initialParentTxHash else acceptedChildren(i)
      val dependentChildren : List[Hash] = chain.txOrphange.getOrphansDependingOn(parentTxHash)
      dependentChildren foreach { dependentChildHash : Hash =>
        val dependentChild = chain.txOrphange.getOrphan(dependentChildHash)
        assert(dependentChild.isDefined)
        try {
          // Try to add to the transaction pool.
          addTransactionToPool(dependentChildHash, dependentChild.get)
          // add the hash to the acceptedChildren so that we can process children of the acceptedChildren as well.
          acceptedChildren.append(dependentChildHash)
        } catch {
          case e : ChainException => {
            if (e.code == ErrorCode.TransactionOutputAlreadySpent) { // The orphan turned out to be a conflicting transaction.
              // do nothing.
              // TODO : Add a test case.
            } else if ( e.code == ErrorCode.ParentTransactionNotFound) { // The transaction depends on another parent transaction.
              // do nothing. Still an orphan transaction.
              // TODO : Add a test case.
            } else {
              throw e
            }
          }
        }
      }
      chain.txOrphange.removeDependenciesOn(parentTxHash)
      i += 1
    } while( i < acceptedChildren.length)

    acceptedChildren foreach { childTxHash : Hash =>
    }

    // Remove duplicate by converting to a set, and return as a list.
    acceptedChildren.toSet.toList
  }

  /**
    * Remove transactions from the indexes maintaining the orphans.
    *
    * @param orphanTransactions The list of hashes of the accepted orphan transactions to remove.
    */
  def delOrphans(orphanTransactions : List[Hash]) : Unit = {
    chain.txOrphange.delOrphans(orphanTransactions)
  }

  /**
    * Add an orphan transaction.
    *
    * @param txHash The hash of the orphan transaction
    * @param transaction The orphan transaction.
    */
  def putOrphan(txHash : Hash, transaction : Transaction) : Unit = {
    chain.txOrphange.putOrphan(txHash, transaction)
  }
}
