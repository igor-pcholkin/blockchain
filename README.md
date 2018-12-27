# blockchain

A blockchain for storing arbitrary facts which could be agreed upon by participants of the blockchain.
The facts include (but not restricted by) financial transactions.

## Key principles:

- Simplicity of implementation
- No cryptocurrency is involved, hence no mining
- No scripts
- No Proof-of-X
- Any block could be created and stored in blockchain by any participant of the blockchain any time
- Any block should be considered as valid by all participants when it meets basic technical criterias and contains only valid facts (transactions)

## Facts

A fact is any idea or claim about any aspect of the world. One example of a fact is a financial transaction. 
A fact is valid if it is signed by all participants of the fact (transaction) and could be verified by any participant of blockchain. A fact can be signed by different number of participants depending on "arity" of the fact. A financial transaction is a kind of fact which should be signed by 2 participants, hence it has arity = 2.
In order to create fact and store it in blockchain a participant of the blockchain should issue a so called "statement" which is a blueprint of a fact. Statement contains some data (e.g. information about amount and currency of money to be transferred and participants of transaction). In case of financial transaction participants of the transaction (mentioned in from and to fields) are also those who should sign the transaction. A statement is not stored in blockchain, instead it is sent to other participants of the statement to be signed. When it is signed by all mentioned participants it becomes a fact (transaction). The new fact then is stored in a newly created block which is then added to the blockchain and propagated to entire network.
