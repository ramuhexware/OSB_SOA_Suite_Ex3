# Oracle WebLogic & SOA Suite Docker Deployment Guide

This project folder contains configuration scripts to pull, launch, and configure **Oracle WebLogic Server** and **Oracle SOA Suite** inside Docker containers, and details how to utilize the Enterprise Manager (EM) console to trace BPEL instances visually.

---

## 1. Prerequisites: Logging in to Oracle Container Registry

Oracle hosts official middleware images on the **Oracle Container Registry (OCR)**. Before launching the containers, you must accept Oracle's License Agreement:
1. Go to [container-registry.oracle.com](https://container-registry.oracle.com/) and sign in with your Oracle Account.
2. Navigate to **Middleware** and accept the license terms for **soa** and **database/express**.
3. Log in to the registry via the command line:
   ```bash
   docker login container-registry.oracle.com
   ```
   *Enter your Oracle Account credentials.*

---

## 2. Launching the Containers

To spin up the Oracle 21c Database (holding MDS/SOAINFRA schemas) and Oracle SOA Suite 12c, execute:
```bash
# Launch the database and middleware servers in detached background mode
docker compose -f docker-compose-soa.yml up -d
```
*Note: The initial domain boot and database RCU schema configuration might take 10-15 minutes depending on host machine hardware.*

---

## 3. Accessing the Administrative Portals

| Console Interface | URL | Credentials | Purpose |
| :--- | :--- | :--- | :--- |
| **WebLogic Server Console** | `http://localhost:7001/console` | `weblogic` / `Welcome1` | Manage domains, servers, data sources (JDBC), and JMS queues. |
| **Enterprise Manager (EM)** | `http://localhost:7001/em` | `weblogic` / `Welcome1` | Monitor deployed composites, trace BPEL instances, and test endpoints. |

---

## 4. How to Check the BPEL Flow UI (Step-by-Step)

The **Flow UI** is a graphical tracer inside Enterprise Manager that allows you to inspect variables and execution states of your BPEL orchestration process. Follow these steps to trace a workflow:

### Step 1: Open Enterprise Manager (EM)
Navigate to `http://localhost:7001/em` in your browser and sign in.

### Step 2: Navigate to your Composite
1. In the left navigation pane, expand the folder structure: **Metadata Repositories** -> **SOA** -> **soa-infra (soa_server1)**.
2. Select your partition (typically **default**).
3. Click on the deployed master composite: **`LoanServiceSOAEngine [1.0]`**.

### Step 3: Trigger a Test Instance
1. At the top of the composite dashboard, click the **Test** button.
2. Input a test XML payload (for example, entering an applicant name, loan amount, and a clean SSN).
3. Click **Test Web Service** at the top right. This submits the payload to the WSDL port and triggers a running BPEL instance.

### Step 4: Open Flow Instances Tab
1. Click the **Flow Instances** tab at the top of the dashboard.
2. You will see a list of executed transactions. Locate your transaction by its timestamp or generated **Instance ID** and click on the ID link.

### Step 5: View the Graphical Flow Trace
1. Click the **Flow Trace** button.
2. The page renders a tree list of invoked partner links. Locate the row containing **`LoanApprovalProcess`** (our BPEL engine component) and click it.
3. Click the **Audit Trail** dropdown and select **Graphical View**.
4. The console renders a flow diagram showing the execution path:
   - Completed activities (like successful `<invoke>` actions) are marked with **green checkmarks**.
   - Executing or waiting activities (like a pending Human Task Manual Review) are highlighted in **blue**.
   - Terminated or faulted activities (like credit score timeouts or watchlist matches) are highlighted with **red warning symbols**.

```
                [ReceiveRequest] (Success)
                       |
                 [CreditScope] (Success)
                       |
               [ValuationScope] (Compensated) <--- Fraud Watchlist triggered
                       |
           (X) [ThrowFraudAlert] (Faulted)
```

### Step 6: Inspect Run-Time Variable Payloads
- Click on any activity block (such as an `<assign>` or `<invoke>`) directly on the flowchart.
- A popup inspector panel displays:
  - **Input Payload**: The exact XML block dispatched to the partner link (e.g. credit check request).
  - **Output Payload**: The return data received from the sub-composite.
  - **Error Stack Trace**: If an activity faulted, the console shows the specific fault namespace and system exception logs.
