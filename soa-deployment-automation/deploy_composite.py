# =========================================================================
# Oracle WebLogic Scripting Tool (WLST) Deployment Automation Script
# =========================================================================
# This script connects to a running WebLogic admin server, targets the 
# soa-infra managed server, and deploys a packaged SAR (SOA Archive).
#
# Usage:
# wlst.cmd deploy_composite.py
# =========================================================================

import sys

# Define variables
username = 'weblogic'
password = 'Welcome1'
adminServerUrl = 't3://localhost:7001'
sarLocation = '../loan-service-soa/deploy/sca_LoanServiceSOAEngine_rev1.0.sar'
partition = 'default'
overwrite = 'true'

print '====================================================================='
print 'Connecting to WebLogic Administration Domain...'
print '====================================================================='
try:
    connect(username, password, adminServerUrl)
    print 'Successfully connected to domain.'
except Exception, e:
    print 'Failed to connect to WebLogic Admin Server at: ' + adminServerUrl
    print str(e)
    sys.exit(1)

print '====================================================================='
print 'Deploying SOA Composite Archive (SAR) to soa-infra engine...'
print '====================================================================='
try:
    # sca_deploy is the built-in WLST command provided by WebLogic's SOA domain extension
    sca_deploy(sarLocation, partition=partition, overwrite=int(overwrite == 'true'), user=username, password=password, serverUrl=adminServerUrl)
    print 'Deployment completed successfully.'
except Exception, e:
    print 'Deployment failed during execution.'
    print str(e)
    sys.exit(1)

# Disconnect from server
disconnect()
print 'Disconnected from WebLogic. Exiting.'
sys.exit(0)
