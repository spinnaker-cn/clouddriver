package com.netflix.spinnaker.clouddriver.hecloud.model

import com.netflix.spinnaker.clouddriver.model.Health
import com.netflix.spinnaker.clouddriver.model.HealthState
import groovy.transform.Canonical

@Canonical
class HeCloudInstanceHealth implements Health {

  final String healthClass = 'platform'
  final String type = 'HeCloud'
  Status instanceStatus

  HealthState getState() {
    instanceStatus.toHealthState()
  }

  enum Status {
    ACTIVE,
    BUILD,
    DELETED,
    ERROR,
    HARD_REBOOT,
    MIGRATING,
    PAUSED,
    REBOOT,
    REBUILD,
    RESIZE,
    REVERT_RESIZE,
    SHUTOFF,
    SHELVED,
    SHELVED_OFFLOADED,
    SOFT_DELETED,
    SUSPENDED,
    VERIFY_RESIZE,
    NORMAL  // scaling instance health state

    HealthState toHealthState() {
      switch (this) {
        case NORMAL:
          return HealthState.Up
        case BUILD:
          return HealthState.Starting
        case ACTIVE:
          return HealthState.Unknown
        case SHUTOFF:
          return HealthState.Down
        default:
          return HealthState.Unknown
      }
    }
  }
}
