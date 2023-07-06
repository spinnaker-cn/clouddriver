package com.netflix.spinnaker.clouddriver.ctyun.deploy.ops

import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.EnableDisableCtyunServerGroupDescription

/*
curl -X POST \
  http://localhost:7002/Ctyun/ops \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
  -d '[
    {
        "disableServerGroup": {
        	"accountName": "test",
            "serverGroupName": "myapp-dev-v007",
            "region": "ap-guangzhou",
            "credentials": "test"
        }
    }
]'
*/

class DisableCtyunServerGroupAtomicOperation extends AbstractEnableDisableAtomicOperation {
  final String basePhase = "DISABLE_SERVER_GROUP"
  boolean disable = true

  DisableCtyunServerGroupAtomicOperation(EnableDisableCtyunServerGroupDescription description) {
    super(description)
  }
}
