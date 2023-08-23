package com.netflix.spinnaker.clouddriver.ctyun.security

import com.netflix.spinnaker.clouddriver.names.NamerRegistry
import com.netflix.spinnaker.clouddriver.security.AccountCredentials
import com.netflix.spinnaker.clouddriver.ctyun.CtyunCloudProvider
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunBasicResource
import com.netflix.spinnaker.clouddriver.ctyun.names.CtyunBasicResourceNamer
import com.netflix.spinnaker.fiat.model.resources.Permissions
import com.netflix.spinnaker.moniker.Namer
import groovy.transform.Canonical
import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j

@Slf4j
@Canonical
@TupleConstructor
class CtyunNamedAccountCredentials implements AccountCredentials<CtyunCredentials> {
  final String cloudProvider = CtyunCloudProvider.ID

  final String name
  final String environment
  final String accountType
  final CtyunCredentials credentials
  List<CtyunRegion> regions

  final List<String> requiredGroupMembership
  final Permissions permissions

  Namer namer = new CtyunBasicResourceNamer()

  CtyunNamedAccountCredentials(
    String name,
    String environment,
    String accountType,
    String accessKey,
    String securityKey,
    List<String> regions,
    /*String accountId,
    String userId,*/
    String clouddriverUserAgentApplicationName
  ){
    this.name = name
    this.environment = environment
    this.accountType = accountType
    //this.credentials = new CtyunCredentials(accessKey, securityKey,accountId,userId)
    this.credentials = new CtyunCredentials(accessKey, securityKey)
    this.regions = buildRegions(regions)
    NamerRegistry.lookup()
      .withProvider(CtyunCloudProvider.ID)
      .withAccount(name)
      .setNamer(CtyunBasicResource.class, namer)
  }

  private static List<CtyunRegion> buildRegions(List<String> regions) {
    regions?.collect {new CtyunRegion(it)} ?: new ArrayList<CtyunRegion>()
  }

  static class CtyunRegion {
    public final String name

    CtyunRegion(String name) {
      if (name == null) {
        throw new IllegalArgumentException("name must be specified.")
      }
      this.name = name
    }

    String getName() {return name}

    @Override
    boolean equals(Object o) {
      if (this == o) return true
      if (o == null || getClass() != o.getClass()) return false

      CtyunRegion region = (CtyunRegion) o

      name.equals(region.name)
    }

    @Override
    int hashCode() {
      name.hashCode()
    }
  }
}
