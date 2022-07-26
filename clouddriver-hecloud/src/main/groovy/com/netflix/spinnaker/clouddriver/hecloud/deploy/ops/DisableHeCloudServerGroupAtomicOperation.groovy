package com.netflix.spinnaker.clouddriver.hecloud.deploy.ops

import com.netflix.spinnaker.clouddriver.hecloud.deploy.description.EnableDisableHeCloudServerGroupDescription

/*
curl -X POST \
  http://localhost:7002/hecloud/ops \
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

class DisableHeCloudServerGroupAtomicOperation extends AbstractEnableDisableAtomicOperation {
  final String basePhase = "DISABLE_SERVER_GROUP"
  boolean disable = true

  DisableHeCloudServerGroupAtomicOperation(EnableDisableHeCloudServerGroupDescription description) {
    super(description)
  }
}
