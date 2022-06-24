package com.netflix.spinnaker.clouddriver.hecloud.deploy.ops

import com.netflix.spinnaker.clouddriver.hecloud.deploy.description.EnableDisableHeCloudServerGroupDescription

/*
curl -X POST \
  http://localhost:7002/hecloud/ops \
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

class EnableHeCloudServerGroupAtomicOperation extends AbstractEnableDisableAtomicOperation {
  final String basePhase = "ENABLE_SERVER_GROUP"
  final boolean disable = false

  EnableHeCloudServerGroupAtomicOperation(EnableDisableHeCloudServerGroupDescription description) {
    super(description)
  }
}
