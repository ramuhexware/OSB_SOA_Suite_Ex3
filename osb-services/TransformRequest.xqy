xquery version "1.0" encoding="utf-8";

(:: OracleServiceBusRepresented XQuery Transformation ::)

declare namespace types = "http://xmlns.oracle.com/LoanOrchestration/types";
declare variable $request as element(types:LoanApplicationRequest) external;

declare function local:transform($request as element(types:LoanApplicationRequest)) as element(types:LoanApplicationRequest) {
    <types:LoanApplicationRequest>
        <types:applicantName>{fn:upper-case(data($request/types:applicantName))}</types:applicantName>
        <types:ssn>{data($request/types:ssn)}</types:ssn>
        <types:loanAmount>{data($request/types:loanAmount)}</types:loanAmount>
        <types:monthlyIncome>{data($request/types:monthlyIncome)}</types:monthlyIncome>
        <types:propertyAddress>{data($request/types:propertyAddress)}</types:propertyAddress>
        <types:propertyValue>{data($request/types:propertyValue)}</types:propertyValue>
    </types:LoanApplicationRequest>
};

local:transform($request)
