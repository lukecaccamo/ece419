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

## Time
- Importance?
  - A point of reference for each machine.
  - Nodes must agree on certain things.
  - Nodes should work independently to achieve the most progress.

### Computer & Network Time
- Computer clocks use a crystal oscillator to keep track of time.
  - Reloads upon interrupt.
  - Real Time Clock (RTC) is used:
    - Active when PC is off
    - Low power
    - IRQ 8
    - AKA "wall clock" time
    - Syncs with the system clock when computer is on
- Universal Time Coordinated (UTC)
  - Worldwide standard (through NTP) and independent from timezones.
  - Coordinated by 400 institutions' estimates of current time using atomic clocks.
    - Very accurate, but very expensive.

### Clock Synchronization
- Clock skew: instantaneous diff between 2 clocks
- Clock drift: rate at which clock skew increases between a clock and some reference clock
- Sync mechanisms:
  - Hardware support: radio reciveres, GPS, atomic clock, shared backplane signals
    - Tight sync but costly
- External synchronization:
  - Request to reference clockL
    - Involves network round trip time (RTT)
    - Client must adjust reponse based on knowledge of network RTT
  - Probabilistic clock sync:
    - Used on LANs and connected to a time reference source (i.e. UTC)
  - Cristian's algorithm:
    - Measures RTT for request to server.
    - Server responds with time values.
    - Client assumes transmission delays split equally and then used to factor in time to process request at server.
    - `t = t' + RTT / 2`
    - This can be done by making multiple requests and processing its delays on the server.
    - Accuracy is `+/- RTT / 2`
    - Assuming network delays are symmetric: `T_new = T_server + (T_1 - T_0) / 2`
    - Accuracy is `+/- (T_1 - T_0) / 2 - T_min` where `T_min` is the min network delay.
    - Keep in mind, errors are cumulative. Node A to Node B has accuracy `+/- 5 ms` and Node B to Node C has `+/- 7 ms`. The net accuracy at Node A to Node C is `+/- 12ms`
    - Problems with Cristian's algorithm:
      - Centralized time server - single point of failure
- Internal synchronization:
  - Berkeley algorithm:
    - Perform internal sync to set clocks of all machines to within a bound.
    - Intended for intranets / LANs
    - Sychronize clocks to within 20-25 ms on LAN
    - Process:
      1. Choose leader via an election process
      2. Leader polls nodes that reply with their time
      3. Leader observes RTT of messages and estimates time of each node and its own
      4. Leader averages clock times, ignoring any outliers
      5. Leader sends out amount to adjust each node
  - Network time protocol:
    - Service that distributes UTC time over networks
    - Hierarchical distributed system which provides scalability, fault-tolerance and security
    - Maintains time within 10 ms over internet
    - Achieves 1 ms accuracy in LANs
    - Stratum 1 time servers are directly connected to reference clock
    - Stratum 2 connects to Stratum 2 and Stratum 3 time servers

### Clock Adjustment
- Must meet requirements:
  - Time must increase monotonically and cannot go backwards
  - Time should not show sudden jumps
- Hardware and software clocks:
  - OS reads time on hardware clock `H_i(t)`
  - Then calculates time on its software clock: `C_i(t) = a * H_i(t) + b`
  - `a` and `b` are adjusted to reach clock adjustment requirements
    - Slow down by not updating the full amount at each clock interrupt
  - *You cannot set local clock to be equal to real time*
  - You must do it in a way to meet monotonicity
  - However, imperfect timing of sync points may lead to sawtooth time behavior (too slow, then too fast)

### Logical Clocks
- Often sufficient to know the order of events instead of full timestamp
- A single process order is determined by instruction execution sequence
- Two process order on the same computer determined useing logical physical clock
- Two process order on difference computer cannot be determined using logical physical clock
- Logical Clock:
  - Abandon idea of physical time, only know order of events
  - First introduced by Lamport
- Happened-before relation (`->`):
  - (causal) ordering of events
  - If `a` and `b` are evenets in the same process and `a` occurred (message sender) before `b` (message reciever) then `a -> b`.
  - `->` is transitive: if `a -> b` and `b -> c` then `a -> c`.
  - If neither `a -> b` nor `b -> a` then `a` and `b` are concurrent, `a || b`.
  - For any two events `a` and `b`, we can say that they are either:
    - `a -> b`;
    - `b -> a`;
    - or `a || b`
- Causality relation:
  - If `a -> b`, event `a` might or might not causally effect event `b`.
  - Concurrent events do not causally effect each other.
- Lamport Clock:
  - Tracks `->` numerically.
  - Each process has a logical clock, the clock can assign a value to any event in a process.
  - The value is the timestamp of the event in the process.
  - Timestamps have no relation to physical time.
  - Logical clocks must be **monotonically increasing timestamps**.
  - Can be implemented with a simple integer counter.
  - Clock must be imcremented before an event occurs, thus, the starting index must be 1 or more.
  - If `a` is the event of the sending message, then `a` is assigned a timestamp.
  - When that same message `m` is recieved by a different process, the new timestamp is the old timestamp + 1.
  - Limitation: **one cannot determine whether two events are causally related from timestamps alone**
    - See vector clocks
- Correctness condition (`C()`):
  - Clock condition: if `a -> b` then `C(a) < C(b)` but not if `C(a) < C(b)` then `a - b`.
  - For any two events in the same process `C(a) < C(b)` implies `a -> b`.
  - For a process sending/recieving messages `C(a) < C(b)` implies `a -> b`.
  - But outside of those two cases, `C(a) < C(b)` **does not imply** `a - b`.
- Total order for events:
  - Let `a` be an event in `P_i` and `b` an event in `P_j`, then `a => b` iff either:
    - `C_i(a) < C_j(b)` or
    - `C_i(a) = C_j(b)` and `P_i < P_j`
- Vector Clocks:
  - Each process keeps an array updated with timestamps of all processes (as a "guess" of their clock).
  - Relations:
    - `C_a = C_b` iff for all `i`: `C_a[i] = C_b[i]`
    - `C_a <= C_b` iff for all `i`: `C_a[i] <= C_b[i]`
    - `C_a < C_b` iff `C_a <= C_b` and for all `i`: `C_a[i] < C_b[i]`
    - `C_a || C_b` iff `NOT(C_a[i] < C_b[i]) AND NOT(C_b[i] < C_a[i])`
  - Let `a`, `b` be two events with vector time stamps `C_a`, `C_b` then:
    - `a -> b` iff `C_a < C_b`
    - `a || b` iff `C_a || C_b`
- Applications of vector clocks in Dynamo: versioning data objects/ version control.
- Summary:
- Clocks that are not based on real-time
- Logical time progresses using events:
  - Local execution of some code
  - Send/receive messages
  - Happened-befor relationship
    - Tracks causality between events across processes
  - Lamport clock
    - Single counter updated at every event
    - Introduces false positive causality
  - Vector clock
    - Size of timestamp relative to # of processes
    - Can determine causality more accurately than Lamport clock