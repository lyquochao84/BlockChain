import java.util.ArrayList;
import java.util.HashMap;

/* Block Chain should maintain only limited block nodes to satisfy the functions
   You should not have the all the blocks added to the block chain in memory 
   as it would overflow memory
 */

public class BlockChain {
   public static final int CUT_OFF_AGE = 10;

   // The block node with the maximum height
   private BlockNode maxHeightNode; 

   // Global transaction pool
   private TransactionPool transactionPool; 

   // Maps block hashes to their corresponding BlockNode
   private HashMap<ByteArrayWrapper, BlockNode> blockMap; 

   // all information required in handling a block in block chain
   private class BlockNode {
      public Block b;
      public BlockNode parent;
      public ArrayList<BlockNode> children;
      public int height;
      // utxo pool for making a new block on top of this block
      private UTXOPool uPool;

      public BlockNode(Block b, BlockNode parent, UTXOPool uPool) {
         this.b = b;
         this.parent = parent;
         children = new ArrayList<BlockNode>();
         this.uPool = uPool;
         if (parent != null) {
            height = parent.height + 1;
            parent.children.add(this);
         } else {
            height = 1;
         }
      }

      public UTXOPool getUTXOPoolCopy() {
         return new UTXOPool(uPool);
      }
   }

   /* create an empty block chain with just a genesis block.
    * Assume genesis block is a valid block
    */
   public BlockChain(Block genesisBlock) {
      // IMPLEMENT THIS
      UTXOPool genesisUTXOPool = new UTXOPool();
      Transaction coinbaseTx = genesisBlock.getCoinbase();
      UTXO utxo = new UTXO(coinbaseTx.getHash(), 0);
      genesisUTXOPool.addUTXO(utxo, coinbaseTx.getOutput(0));

      BlockNode genesisNode = new BlockNode(genesisBlock, null, genesisUTXOPool);

      maxHeightNode = genesisNode;
      transactionPool = new TransactionPool();
      blockMap = new HashMap<>();
      blockMap.put(new ByteArrayWrapper(genesisBlock.getHash()), genesisNode);
   }

   /* Get the maximum height block
    */
   public Block getMaxHeightBlock() {
      // IMPLEMENT THIS
      return maxHeightNode.b;
   }
   
   /* Get the UTXOPool for mining a new block on top of 
    * max height block
    */
   public UTXOPool getMaxHeightUTXOPool() {
      // IMPLEMENT THIS
      return maxHeightNode.getUTXOPoolCopy();
   }
   
   /* Get the transaction pool to mine a new block
    */
   public TransactionPool getTransactionPool() {
      // IMPLEMENT THIS
      return transactionPool;
   }

   /* Add a block to block chain if it is valid.
    * For validity, all transactions should be valid
    * and block should be at height > (maxHeight - CUT_OFF_AGE).
    * For example, you can try creating a new block over genesis block 
    * (block height 2) if blockChain height is <= CUT_OFF_AGE + 1. 
    * As soon as height > CUT_OFF_AGE + 1, you cannot create a new block at height 2.
    * Return true of block is successfully added
    */
   public boolean addBlock(Block b) {
      if (b == null || b.getPrevBlockHash() == null) {
         return false;
      }
 
      BlockNode parentNode = blockMap.get(new ByteArrayWrapper(b.getPrevBlockHash()));
      if (parentNode == null) {
            return false; // Parent block does not exist
      }
   
      // Early height constraint check
      if (parentNode.height + 1 <= maxHeightNode.height - CUT_OFF_AGE) {
            return false;
      }
   
      UTXOPool parentUTXOPool = parentNode.getUTXOPoolCopy();
      TxHandler txHandler = new TxHandler(parentUTXOPool);
      ArrayList<Transaction> txList = b.getTransactions();
   
      Transaction[] validTxs = txHandler.handleTxs(txList.toArray(new Transaction[0]));
      if (validTxs.length != txList.size()) {
            return false; // Some transactions are invalid
      }
   
      // Create a new UTXOPool for the new block
      UTXOPool newUTXOPool = txHandler.getUTXOPool();
      Transaction coinbaseTx = b.getCoinbase();
      UTXO coinbaseUTXO = new UTXO(coinbaseTx.getHash(), 0);
      newUTXOPool.addUTXO(coinbaseUTXO, coinbaseTx.getOutput(0));
   
      BlockNode newNode = new BlockNode(b, parentNode, newUTXOPool);
   
      // Add the block to the map
      blockMap.put(new ByteArrayWrapper(b.getHash()), newNode);
   
      // Update the max height node if necessary
      if (newNode.height > maxHeightNode.height) {
            maxHeightNode = newNode;
      }
   
      // Remove processed transactions from the transaction pool
      for (Transaction tx : b.getTransactions()) {
            transactionPool.removeTransaction(tx.getHash());
      }
   
      return true;
   }

   /* Add a transaction in transaction pool
    */
   public void addTransaction(Transaction tx) {
      // IMPLEMENT THIS
      transactionPool.addTransaction(tx);
   }
}
