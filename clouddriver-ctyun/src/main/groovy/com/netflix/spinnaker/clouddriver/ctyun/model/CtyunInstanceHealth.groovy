package com.netflix.spinnaker.clouddriver.ctyun.model

import com.netflix.spinnaker.clouddriver.model.Health
import com.netflix.spinnaker.clouddriver.model.HealthState
import groovy.transform.Canonical

@Canonical
class CtyunInstanceHealth implements Health {

  final String healthClass = 'platform'
  final String type = 'Ctyun'
  Status instanceStatus

  HealthState getState() {
    instanceStatus.toHealthState()
  }

  enum Status {
    /*PENDING,
    LAUNCH_FAILED,
    RUNNING,
    STOPPED,
    STARTING,
    STOPPING,
    REBOOTING,
    SHUTDOWN,
    TERMINATING,
    BACKINGUP,
    EXPIRED,
    FREEZING,
    REBUILD*/
    BACKINGUP,
    RESTARTING,
    CREATING,
    RUNNING,
    EXPIRED,
    STARTING,
    FREEZING,
    STOPPED,
    REBUILD,
    STOPPING
    /*backingup	备份中	restarting	重启中
    creating	创建中	running	运行中
    expired	已到期	starting	执行开机中
    freezing	冻结中	stopped	已关机
    rebuild	重装	stopping	执行关机中*/

    HealthState toHealthState() {
      switch (this) {
        /*case PENDING:
          return HealthState.Starting*/
        case STARTING:
          return HealthState.Starting
        /*case RUNNING:
          return HealthState.Unknown*/
        case STOPPED:
          return HealthState.Down
        case RUNNING:
          return HealthState.Up
        /*case BACKINGUP:
          return HealthState.OutOfService
        case RESTARTING:
          return HealthState.OutOfService
        case CREATING:
          return HealthState.OutOfService
        case EXPIRED:
          return HealthState.OutOfService
        case STARTING:
          return HealthState.Starting
        case FREEZING:
          return HealthState.OutOfService
        case STOPPED:
          return HealthState.Down
        case REBUILD:
          return HealthState.Starting
        case STOPPING:
          return HealthState.OutOfService*/
        default:
          return HealthState.Unknown
      }
    }
  }
}
