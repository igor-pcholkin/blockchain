# blockchain

A lightweight general-purpose blockchain solution for storing arbitrary facts which could be agreed upon by participants of the blockchain.
The facts include (but not restricted to) financial transactions.

## Key principles:

- Simplicity of implementation
- No cryptocurrency is involved, hence no mining
- No scripts
- No Proof-of-X
- Any block could be created and stored in blockchain by any participant of the blockchain any time
- Any block should be considered as valid by all participants when it meets basic technical criterias and contains only valid facts (e.g. transactions)
- Identification of participants (users) - by public keys. It is possible to link some personal info to public key, but that is not mandatory and depends on specific business needs

## Facts

A fact is any idea or claim about any aspect of the world. One example of a fact is a financial transaction. 
Suppose Bob wants to borrow some money from Alice. They meet offline and Alice gives money to Bob. Now they can create some kind of "contract" which states obligations of Bob, whether he should return money, when, how much etc. The contract could be registered in blockchain as a fact and signed by both participants (using their corresponding private keys). Unlike bitcoin there is no real money transfer in blockchain itself (if we can call bitcoin money transfers real) it is only registration of fact that the transfer of money occurred between the participants. Whether money transfer really occurred between them is up to participants themselves. What is real however is a "fixation of agreement" between those two participants that the money transfer really occurred. Once fixed as a fact in a blockchain that agreement is undisputed and could be verified by both participants and any other users of the blockchain.

A fact is valid if it is signed by all participants of the fact (transaction) and could be verified by any participant of blockchain. A fact can be signed by different number of participants depending on "arity" of the fact. E.g. we can decide about a financial transaction is a kind of fact which should be signed by 2 participants, hence it has arity = 2.

In order to create fact and store it in blockchain a participant of the blockchain should issue a so called "statement" which is a blueprint of a fact. Statement contains some data (e.g. information about amount and currency of money to be transferred and participants of transaction). In case of financial transaction participants of the transaction (mentioned in from and to fields) are also those who should sign the transaction. A statement is not stored in blockchain, instead it is sent to other participants of the statement to be signed. When it is signed by all mentioned participants it becomes a fact (transaction). The new fact then is stored in a newly created block which is then added to the blockchain and propagated to entire network.

The application at the moment supports three kinds of facts:
- Payments: lightweight contracts which allow to fix as a fact that some money transfer occurred between participants A and B. The only information that can be stored within payment are: amount (and currency) of money transferred, public key of sender, public key of receiver, time of transaction. This fact to be valid requires 2 signatures (those of participants of financial transaction)
- Registered users: allows to link some personal data (name, email, photo, etc.) with public key of participant to simplify user search/identification. This kind of fact requires only one signature - signature of user who registered that user (it could be the same person).
- Approved facts: allow any user to approve any previously registered fact in blockchain. For example one user can approve another registered user "marking" him as trusted one. This kind of fact requires only one signature - signature of approving user

## Scope of the project

Current project contains a framework for building fact-based blockchains in JVM-compatible programming languages.
It also contains a sample application (client) based on the framework and which can run on java-enabled desktop or server computer. The application is a blockchain client (and server) which allows:
- create blocks and store arbitrary data in it (structured as so called "facts").
- create users (key pairs) and store them as application data.
- exchange blocks and another service messages with another nodes - instances of the same application running on the same physical computer or another one. For simplicity that communication is done via HTTP.
- remote control and basic monitoring via http server endpoints

## Current application uses

Uses of application present in the project is determined only by a current (core) set of fact types. 
For now it is possible:
- register users and create "chains of trust" between them
- register ad-hoc money transfers between users

When introducing new facts and creating new http endpoints supporting them a scope of the application uses could be extended almost infinitely.

## Running the app

- Download sbt:
https://www.scala-sbt.org/download.html

- In the project root directory run the following command:
sbt "run Riga 8551"


