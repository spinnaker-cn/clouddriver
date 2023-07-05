package com.netflix.spinnaker.clouddriver.ctyun.deploy.ops

import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.EnableDisableCtyunServerGroupDescription

/*
curl -X POST \
  http://localhost:7002/Ctyun/ops \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
  -d '[
    {
        "enableServerGroup": {
        	"accountName": "test",
            "serverGroupName": "myapp-dev-v007",
            "region": "ap-guangzhou",
            "credentials": "test"
        }
    }
]'
*/

class EnableCtyunServerGroupAtomicOperation extends AbstractEnableDisableAtomicOperation {
  final String basePhase = "ENABLE_SERVER_GROUP"
  final boolean disable = false

  EnableCtyunServerGroupAtomicOperation (EnableDisableCtyunServerGroupDescription description) {
    super(description)
  }
}
