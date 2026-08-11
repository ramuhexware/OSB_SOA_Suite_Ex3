# BPEL Workflow Document: CreditProcess.bpel

This document contains the visual workflow representation of the BPEL process from `CreditProcess.bpel`.

## Flow Diagram

```mermaid
graph TD
    node1[["Receive: receiveInput<br/>(processCreditCheck)"]]
    node2["Assign: CalculateScore"]
    node3[["Reply: replyOutput<br/>(processCreditCheck)"]]
    node1 --> node2
    node2 --> node3
    start_node --> node1
    node3 --> end_node
```

*Generated automatically using bpel_to_mermaid.py.*
