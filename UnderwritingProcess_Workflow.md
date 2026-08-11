# BPEL Workflow Document: UnderwritingProcess.bpel

This document contains the visual workflow representation of the BPEL process from `UnderwritingProcess.bpel`.

## Flow Diagram

```mermaid
graph TD
    node1[["Receive: receiveInput<br/>(processUnderwriting)"]]
    node2{"If: EvaluateRules<br/>($inputVar.payload/ns1:creditScore >= 700 and (($inputVar.payload/ns1:loanAmount div $inputVar.payload/ns1:propertyValue) < 0.8))"}
    node3[Join EvaluateRules]
    node4["Assign: AssignAutoApproved"]
    node2 -- "True" --> node4
    node4 --> node3
    node5{"ElseIf: ($inputVar.payload/ns1:creditScore < 500)"}
    node2 -- "False" --> node5
    node6["Assign: AssignAutoRejected"]
    node5 -- "True" --> node6
    node6 --> node3
    node7["Assign: AssignManualReview"]
    node5 -- "False / Else" --> node7
    node7 --> node3
    node8[["Reply: replyOutput<br/>(processUnderwriting)"]]
    node1 --> node5
    node3 --> node8
    start_node --> node1
    node8 --> end_node
```

*Generated automatically using bpel_to_mermaid.py.*
