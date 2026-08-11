# BPEL Workflow Document: DisbursementProcess.bpel

This document contains the visual workflow representation of the BPEL process from `DisbursementProcess.bpel`.

## Flow Diagram

```mermaid
graph TD
    node1[["Receive: receiveInput<br/>(processDisbursement)"]]
    node2["Assign: AssignDisbursed"]
    node3[["Reply: replyOutput<br/>(processDisbursement)"]]
    node1 --> node2
    node2 --> node3
    start_node --> node1
    node3 --> end_node
```

*Generated automatically using bpel_to_mermaid.py.*
