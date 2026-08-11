# BPEL Workflow Document: NotificationProcess.bpel

This document contains the visual workflow representation of the BPEL process from `NotificationProcess.bpel`.

## Flow Diagram

```mermaid
graph TD
    node1[["Receive: receiveInput<br/>(processNotification)"]]
    node2["Assign: AssignNotified"]
    node3[["Reply: replyOutput<br/>(processNotification)"]]
    node1 --> node2
    node2 --> node3
    start_node --> node1
    node3 --> end_node
```

*Generated automatically using bpel_to_mermaid.py.*
