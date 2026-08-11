# Deploying BPEL Applications on Oracle WebLogic Server

This folder contains automation scripts and reference materials demonstrating how Oracle SOA Suite BPEL composite applications are packaged, deployed, and managed inside an **Oracle WebLogic Server (WLS)** domain.

---

## 1. The Deployment Format: SAR (SOA Archive)

Oracle SOA Suite applications are packaged as **SAR (SOA Archive)** files (e.g. `sca_LoanServiceSOAEngine_rev1.0.sar`).
- A SAR file is structurally a standard ZIP/JAR file containing compiled BPEL instructions (`.bpel`), interface definitions (`.wsdl`), schemas (`.xsd`), and the SCA architecture file (`composite.xml`).
- JDeveloper compiled classes, transformation templates (XSLT, XQuery), and custom business rules are compiled directly into the root level of the SAR file.

---

## 2. Under the Hood: How it Works on WebLogic

When a SAR file is deployed to Oracle WebLogic Server, it targets the **`soa_server` managed server** where the **`soa-infra`** (SOA Infrastructure) application is running. The server executes the following pipeline:

```
+------------------+     1. WSDL / XSD Registry      +--------------------------+
|  Packaged SAR    +-------------------------------->| MDS (Metadata Store)     |
+--------+---------+                                 +--------------------------+
         |
         | 2. Deploy Executable
         v
+--------+---------+     3. Wire services & bindings +--------------------------+
|   BPEL Engine    +-------------------------------->| WebLogic SOAP Binding    |
+------------------+                                 +--------------------------+
```

### A. Metadata Store (MDS) Registration
WebLogic registers all shared schemas (`.xsd`) and interface contracts (`.wsdl`) inside the **Oracle MDS (Metadata Store)** database schema. This permits different composites to reference shared resources (such as standard schemas) at runtime without embedding duplicate files inside each SAR.

### B. BPEL Engine Deployment
The BPEL processes (`.bpel`) are loaded by the WebLogic BPEL Engine. The engine:
- Compiles the BPEL XML elements into executable Java bytecode.
- Registers transactional parameters (e.g., whether the process requires a JTA global transaction or participates in one).
- Registers correlation sets and database persistence listeners to manage long-running suspended process instances.

### C. Service Endpoint Binding
The WebLogic binding layer exposes the interfaces specified in `composite.xml` (such as `<binding.ws>`) as active web services (SOAP or REST endpoints) managed by the WebLogic servlet engine.

---

## 3. How to Deploy: Three Methods

This folder contains pre-configured scripts for three standard enterprise deployment paths:

### Method A: Maven Deployment
Ensure the WebLogic and SOA Suite Maven plugins are registered in your local maven repository, then execute:
```bash
# Package the project into a SAR
mvn package

# Deploy the packaged SAR to the active WebLogic server
mvn deploy
```
*Configured in [pom.xml](file:///c:/ramu/Project_Assignment/RapidX/FreddeMac_Project_RapidX/Work/OSB_SOA_Suite_Research_Notes/OSB_SOA_Suite_Ex3/soa-deployment-automation/pom.xml)*

### Method B: WLST (WebLogic Scripting Tool) Python Deployment
WLST is a Python-based command-line scripting environment provided by Oracle.
To run the deploy script:
```bash
# Load WLST environment
C:\Oracle\Middleware\Oracle_Home\oracle_common\common\bin\wlst.cmd deploy_composite.py
```
*Configured in [deploy_composite.py](file:///c:/ramu/Project_Assignment/RapidX/FreddeMac_Project_RapidX/Work/OSB_SOA_Suite_Research_Notes/OSB_SOA_Suite_Ex3/soa-deployment-automation/deploy_composite.py)*

### Method C: Ant Deployment
Using the classic Ant script mapping provided by the SOA Middleware installation:
```bash
# Run ant deployment targets
ant -f build-deploy.xml
```
*Configured in [build-deploy.xml](file:///c:/ramu/Project_Assignment/RapidX/FreddeMac_Project_RapidX/Work/OSB_SOA_Suite_Research_Notes/OSB_SOA_Suite_Ex3/soa-deployment-automation/build-deploy.xml)*

---

## 4. Monitoring & Verifying Deployments

Once deployed, composites are monitored via the **Enterprise Manager (EM) Fusion Middleware Control Console**:
1. Open your browser and navigate to the administration console:
   `http://<admin-host>:<admin-port>/em` (typically `http://localhost:7001/em`).
2. Expand the **SOA** foldout in the left pane and click on **soa-infra**.
3. Under the **Deployed Composites** tab, verify that `LoanServiceSOAEngine [1.0]` is displayed with an **Active** status.
4. Select the composite to view performance metrics, click **Test** to dispatch SOAP payloads, or trace active BPEL execution instances in real-time.
