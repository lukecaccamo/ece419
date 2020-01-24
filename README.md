- Office hours: Monday, 6-7 PM (by appointments)
- There are lectures, tutorial lectures (for assignments), four milestones, and four assignments.
- The assignments prepares you for the exam and the midterm.
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
- BigTable
  - Key-value stores:
    - Containers for key-value pairs
    - Distributed, multi-component, systems
    - NoSQL semantics (non-relational)
    - Offer simpler query semantics in exchange for increased scalability, speed, availability, and flexibility.
    - No schema, raw byte access, no relations, single-row operations
- 