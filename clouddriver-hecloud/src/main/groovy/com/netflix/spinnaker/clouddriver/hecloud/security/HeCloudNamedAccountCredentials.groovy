package com.netflix.spinnaker.clouddriver.hecloud.security

import com.netflix.spinnaker.clouddriver.names.NamerRegistry
import com.netflix.spinnaker.clouddriver.security.AccountCredentials
import com.netflix.spinnaker.clouddriver.hecloud.config.HeCloudConfigurationProperties
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudProvider
import com.netflix.spinnaker.clouddriver.hecloud.model.HeCloudBasicResource
import com.netflix.spinnaker.clouddriver.hecloud.names.HeCloudBasicResourceNamer
import com.netflix.spinnaker.fiat.model.resources.Permissions
import com.netflix.spinnaker.moniker.Namer
import groovy.transform.Canonical
import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j

@Slf4j
@Canonical
@TupleConstructor
class HeCloudNamedAccountCredentials implements AccountCredentials<HeCloudCredentials> {
  final String cloudProvider = HeCloudProvider.ID

  final String name
  final String environment
  final String accountType
  final HeCloudCredentials credentials
  List<HeCloudRegion> regions

  final List<String> requiredGroupMembership
  final Permissions permissions

  Namer namer = new HeCloudBasicResourceNamer()

  HeCloudNamedAccountCredentials(
    String name,
    String environment,
    String accountType,
    String accessKeyId,
    String accessSecretKey,
    List<HeCloudConfigurationProperties.ManagedAccount.Region> regions,
    String clouddriverUserAgentApplicationName
  ){
    this.name = name
    this.environment = environment
    this.accountType = accountType
    this.credentials = new HeCloudCredentials(accessKeyId, accessSecretKey)
    this.regions = buildRegions(regions)
    NamerRegistry.lookup()
      .withProvider(HeCloudProvider.ID)
      .withAccount(name)
      .setNamer(HeCloudBasicResource.class, namer)
  }

  private static List<HeCloudRegion> buildRegions(List<HeCloudConfigurationProperties.ManagedAccount.Region> regions) {
    regions?.collect {new HeCloudRegion(it.name, it.availabilityZones)} ?: new ArrayList<HeCloudRegion>()
  }

  static class HeCloudRegion {
    public final String name
    public final List<String> availabilityZones
    public final List<String> preferredZones

    HeCloudRegion(String name, List<String> availabilityZones) {
      if (name == null) {
        throw new IllegalArgumentException("name must be specified.")
      }
      this.name = name
      this.availabilityZones = availabilityZones
      this.preferredZones = availabilityZones
    }

    String getName() {return name}

    @Override
    boolean equals(Object o) {
      if (this == o) return true
      if (o == null || getClass() != o.getClass()) return false

      HeCloudRegion region = (HeCloudRegion) o

      name.equals(region.name)
    }

    @Override
    int hashCode() {
      name.hashCode()
    }
  }
}
