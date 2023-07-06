package com.netflix.spinnaker.clouddriver.ctyun.provider

import com.netflix.spinnaker.cats.agent.Agent
import com.netflix.spinnaker.cats.agent.AgentSchedulerAware
import com.netflix.spinnaker.clouddriver.cache.SearchableProvider
import com.netflix.spinnaker.clouddriver.ctyun.cache.Keys

import static com.netflix.spinnaker.clouddriver.ctyun.cache.Keys.Namespace.*

class CtyunInfrastructureProvider extends AgentSchedulerAware implements SearchableProvider {

  final String providerName = CtyunInfrastructureProvider.name
  final Collection<Agent> agents

  final Set<String> defaultCaches = [
    APPLICATIONS.ns,
    CLUSTERS.ns,
    INSTANCES.ns,
    LOAD_BALANCERS.ns,
    SECURITY_GROUPS.ns,
    SERVER_GROUPS.ns,
  ].asImmutable()

  CtyunInfrastructureProvider(Collection<Agent> agents) {
    this.agents = agents
  }

  final Map<String, String> urlMappingTemplates = [
    (SECURITY_GROUPS.ns): '/securityGroups/$account/$provider/$name?region=$region'
  ]

  final Map<SearchableResource, SearchResultHydrator> searchResultHydrators = Collections.emptyMap()

  @Override
  Map<String, String> parseKey(String key) {
    return Keys.parse(key)
  }
}
