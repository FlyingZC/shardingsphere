+++
pre = "<b>3.2. </b>"
title = "Distributed Transaction"
weight = 2
chapter = true
+++

## Background

Database transactions should satisfy the features of ACID (atomicity, consistency, isolation and durability).

- Atomicity: transactions are executed as a whole, and either all or none is executed.
- Consistency: transactions should ensure that the state of data remains consistent after the transition.
- Isolation: when multiple transactions execute concurrently, the execution of one transaction should not affect the execution of others.
- Durability: when a transaction committed modifies data, the operation will be saved persistently.

In single data node, transactions are only restricted to the access and control of single database resources, called local transactions. Almost all the mature relational databases have provided native support for local transactions. But in distributed application situations based on micro-services, more and more of them require to include multiple accesses to services and the corresponding database resources in the same transaction. As a result, distributed transactions appear.

Though the relational database has provided perfect native ACID support, it can become an obstacle to the system performance under distributed situations. How to make databases satisfy ACID features under distributed situations or find a corresponding substitute solution, is the priority work of distributed transactions.

## Challenge

For different application situations, developers need to reasonably weight the performance and the function between all kinds of distributed transactions.

Highly consistent transactions do not have totally the same API and functions as soft transactions, and they cannot switch between each other freely and invisibly. The choice between highly consistent transactions and soft transactions as early as development decision-making phase has sharply increased the design and development cost.

Highly consistent transactions based on XA is relatively easy to use, but is not good at dealing with long transaction and high concurrency situation of the Internet. With a high access cost, soft transactions require developers to transform the application and realize resources lock and backward compensation.

## Goal

The main design goal of the distributed transaction modular of Apache ShardingSphere is to integrate existing mature transaction cases to provide an unified distributed transaction interface for local transactions, 2PC transactions and soft transactions; compensate for the deficiencies of current solutions to provide a one-stop distributed transaction solution.

## How it works

ShardingSphere provides begin/ commit/rollback traditional transaction interfaces externally, and provides distributed transaction capabilities through LOCAL, XA and BASE modes.

### LOCAL Transaction

LOCAL mode is implemented based on ShardingSphere's proxy database interfaces, that is begin/commit/rolllback.
For a logical SQL, ShardingSphere starts transactions on each proxied database with the begin directive, executes the actual SQL, and performs commit/rollback.
Since each data node manages its own transactions, there is no coordination and communication between them, and they do not know whether other data node transactions succeed or not.
There is no loss in performance, but strong consistency and final consistency cannot be guaranteed.

### XA Transaction

XA transaction adopts the concepts including AP(application program), TM(transaction manager) and RM(resource manager) to ensure the strong consistency of distributed transactions. Those concepts are abstracted from [DTP mode](http://pubs.opengroup.org/onlinepubs/009680699/toc.pdf) which is defined by X/OPEN group.
Among them, TM and RM use XA protocol to carry out both-way communication, which is realized through two-phase commit.
Compared to traditional local transactions, XA transaction adds a preparation stage where the database can also inform the caller whether the transaction can be committed, in addition to passively accepting commit instructions.
TM can collect the results of all branch transactions and make atomic commit at the end to ensure the strong consistency of transactions.

![Two-phase commit model](https://shardingsphere.apache.org/document/current/img/transaction/overview.png)

ShardingSphere implements XA transactions through a pluggable JTA transaction manager provider and wraps each storage unit as an XA data source.
When a logical SQL statement is executed, the `XAResource` for each physical connection is enlisted in the current JTA transaction,
and the transaction manager coordinates the prepare, commit, or rollback operations of all transaction branches.
Distributed transactions based on XA protocol are more suitable for short transactions with fixed execution time because the required resources need to be locked during execution.
For long transactions, data exclusivity during the entire transaction will have an impact on performance in concurrent scenarios.

### BASE Transaction

If a transaction that implements ACID is called a rigid transaction, then a transaction based on a BASE transaction element is called a flexible transaction.
BASE stands for basic availability, soft state, and eventual consistency.

- Basically Available: ensure that distributed transaction parties are not necessarily online at the same time.
- Soft state: system status updates are allowed to have a certain delay, and the delay may not be recognized by customers.
- Eventually consistent: guarantee the eventual consistency of the system by means of messaging.

ShardingSphere BASE transactions use Seata AT mode. A branch transaction releases its local database lock after committing the local transaction in phase one,
but its global write lock is retained until the global transaction finishes. Therefore, BASE does not hold local database locks for the entire transaction as XA does,
but writes to hot data can still contend for global locks. The throughput benefit from relaxed consistency requirements depends on the workload.

ACID-based strong consistency transactions and BASE-based final consistency transactions are not a jack of all trades and can fully leverage their advantages in the most appropriate scenarios.
Apache ShardingSphere integrates the operational scheme taking SEATA as the flexible transaction.
The following table can be used for comparison to help developers choose the suitable technology.

|                         | *LOCAL*                                      | *XA*                                      | *BASE*                              |
|-------------------------|----------------------------------------------|-------------------------------------------|-------------------------------------|
| Business transformation | None                                         | None                                      | Seata Server needed                 |
| Consistency             | Not supported                                | Supported                                 | Final consistency                   |
| Isolation               | Not supported                                | Supported                                 | Seata AT global locks                |
| Concurrent performance  | no loss                                      | severe loss                               | slight loss                         |
| Applied scenarios       | Inconsistent processing by the business side | short transaction & low-level concurrency | eventual consistency & low hot-spot contention |

### Implicit Distributed Transactions

When a connection is in auto-commit mode, the default transaction type is XA or BASE, no distributed transaction is active, and a write DML statement is routed to multiple execution units,
ShardingSphere-JDBC and ShardingSphere-Proxy automatically start a distributed transaction for that statement. The transaction is committed when the statement succeeds and rolled back when it fails.
This mechanism does not apply to queries or write DML statements routed to only one execution unit.

### Savepoint

In a connection-held transaction, ShardingSphere-JDBC supports `Connection#setSavepoint`, `Connection#rollback(Savepoint)`, and `Connection#releaseSavepoint`,
while ShardingSphere-Proxy supports the `SAVEPOINT`, `ROLLBACK TO SAVEPOINT`, and `RELEASE SAVEPOINT` statements.
Savepoint operations are applied to physical connections already cached by the transaction. A savepoint is also replayed on physical connections acquired later in the transaction.

A LOCAL transaction must have auto-commit disabled, and an XA distributed transaction must already be active. BASE transactions do not hold physical connections and therefore do not support savepoints.
Actual support also depends on the storage database and XA provider's savepoint capabilities.

## Application Scenarios

The database's transactions can meet ACID business requirements in a standalone application scenario. However, in distributed scenarios, traditional database solutions cannot manage and control global transactions, and users may find data inconsistency on multiple database nodes.

ShardingSphere distributed transaction makes it easier to process distributed transactions and provides flexible and diverse solutions. Users can select the distributed transaction solutions that best fit their business scenarios among LOCAL, XA, and BASE modes.

### Application Scenarios for ShardingSphere XA Transactions

Strong data consistency is guaranteed in a distributed environment in terms of XA transactions. However, its performance may be degraded due to the synchronous blocking problem. It applies to business scenarios that require strong data consistency and low concurrency performance.

### Application Scenarios for ShardingSphere BASE Transaction

BASE transactions provide eventual consistency in a distributed environment.
Local database locks are released after phase one, but global write locks are retained until the global transaction finishes.
They are suitable when temporary data inconsistency is acceptable and contention on hot writes is controlled.

### Application Scenarios for ShardingSphere LOCAL Transaction

In terms of LOCAL transactions, the data consistency and isolation among database nodes are not guaranteed in a distributed environment. Therefore, the business sides need to handle the inconsistencies by themselves. This applies to business scenarios where users would like to handle data inconsistency in a distributed environment by themselves.

## Related references
- [YAML distributed transaction configuration](/en/user-manual/shardingsphere-jdbc/yaml-config/rules/transaction/)
