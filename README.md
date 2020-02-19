# ECE419

## Logistics
- Office hours: Monday, 6-7 PM (by appointments)
- There are lectures, tutorial lectures (for assignments), four milestones, and four assignments.
- The assignments prepares you for the exam and the midterm.

## Overview
- Distributed Systems Overview
  - Distributed System Definition: System comprised of several physical disjoint compute resources interconnected by a network.
    - MapReduce (Hadoop, Spark)
    - Peer-to-peer (Bitcoin, BitTorrent)
    - Google infrastructure (Big Table)
    - World Wide Web (Akamai CDN)
  - Build a distributed system to centralize the system.
    - Horizontal scaling costs less.
    - Availability and redundancy.
    - Single point of failure.
  - Related to:
    - Networking (latency, communication)
    - Databases (transactions, consistency)
    - Security (faulty stacks, privacy/encryption)
    - Parallel computing (concurrency, decomposition)
- Reliability
  - Probability of a system to perform its required functions under stated conditions for a specified period of time.
  - To run continuously without failure.
- Availability
  - Proportion of time a sstem is in a functioning state.
  - Ratio of time usable over entire time.
  - Specified as decimal or percentage in class of 9's.
- What's the difference?
  - System going down 1 ms every 1 hr has an availablility of more than  99.9999%. Highly available, yet unreliable.
- Distributed Systems Design Fallacies:
  - Assumpltions designers make that turn out to be false.
  1. Network is reliable
  2. Latency is zero
  3. Bandwidth is infinite
  4. Network is secure
  5. Topology doesn't change
  6. There is one admin
  7. Transport cost is zero
  8. Network is homogeneous
- BigTable:
  - Key-value stores:
    - Containers for key-value pairs
    - Distributed, multi-component, systems
    - NoSQL semantics (non-relational)
    - Offer simpler query semantics in exchange for increased scalability, speed, availability, and flexibility.
    - No schema, raw byte access, no relations, single-row operations
  - Scales horizontally, performant, and adaptable to changing data definitions.
  - Reliable and provides failure recovery, and widely available.
  - Properties:
    - Failure recovery
    - Replication
    - Memory store & write ahead log
    - Versioning
  - BigTable composed of:
    - Client library
    - Master
      - Metadata ops
      - Load balancing: assigns tablets to tablet servers, and detects addition/expiration of tablet servers.
    - Tablet server
      - Data ops: sets of tablets that handles read and write requests
    - Google Filesystem: data & log storage and its replication
    - Chubby: lock service and metadata storage
    - Scheduler: monitors and handles failover
- Apache HBase is the open-source implementation of BigTable:
  - Google Filesystem -> HDFS
  - Chubby -> Zookeeper
  - BigTable -> HBase
  - MapReduce -> Hadoop
- BigTable vs. MapReduce:
  - Both layered on top of GFS
  - Data storage & access vs. batch analytics
  - Read/write web data vs. offline batch processing
- Cassandra:
  - Developed by Facebook and based on Amazon Dynamo
  - Structured storage nodes
  - Decentralized architecture
  - Consistent hashing for load balancing
  - Eventual consistency
  - Uses "gossiping" to exchange data