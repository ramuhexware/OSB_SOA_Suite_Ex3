# BPEL Workflow Document: ValuationProcess.bpel

This document contains the visual workflow representation of the BPEL process from `ValuationProcess.bpel`.

## Flow Diagram

```mermaid
graph TD
    node1{"Pick: PickOperation"}
    node2[Join PickOperation]
    node3(("OnMessage: appraise<br/>(valuationprocess_client)"))
    node4["Assign: AssignAppraisedValue"]
    node5[["Reply: replyAppraise<br/>(appraise)"]]
    node4 --> node5
    node3 --> node4
    node1 --> node3
    node5 --> node2
    node6(("OnMessage: refund<br/>(valuationprocess_client)"))
    node7["Assign: AssignRefundStatus"]
    node8[["Reply: replyRefund<br/>(refund)"]]
    node7 --> node8
    node6 --> node7
    node1 --> node6
    node8 --> node2
    start_node --> node1
    node2 --> end_node
```

*Generated automatically using bpel_to_mermaid.py.*
