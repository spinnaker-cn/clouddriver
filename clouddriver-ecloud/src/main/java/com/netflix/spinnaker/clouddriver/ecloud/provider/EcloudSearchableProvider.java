package com.netflix.spinnaker.clouddriver.ecloud.provider;

import static com.netflix.spinnaker.clouddriver.core.provider.agent.Namespace.CLUSTERS;
import static com.netflix.spinnaker.clouddriver.core.provider.agent.Namespace.LOAD_BALANCERS;
import static com.netflix.spinnaker.clouddriver.core.provider.agent.Namespace.SERVER_GROUPS;
import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.SECURITY_GROUPS;

import com.netflix.spinnaker.cats.agent.Agent;
import com.netflix.spinnaker.cats.agent.AgentSchedulerAware;
import com.netflix.spinnaker.clouddriver.cache.SearchableProvider;
import com.netflix.spinnaker.clouddriver.ecloud.cache.Keys;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EcloudSearchableProvider extends AgentSchedulerAware implements SearchableProvider {
  private final String providerName;

  private final List<Agent> agents;

  private final Set<String> defaultCaches;

  private final Map<String, String> urlMappingTemplates;

  private final Map<SearchableResource, SearchResultHydrator> searchResultHydrators;

  public EcloudSearchableProvider(List<Agent> agents) {
    this.providerName = EcloudSearchableProvider.class.getName();
    this.agents = agents;

    List<String> nsList =
        Arrays.asList(
            Keys.Namespace.APPLICATIONS.ns,
            Keys.Namespace.CLUSTERS.ns,
            Keys.Namespace.INSTANCES.ns,
            Keys.Namespace.LOAD_BALANCERS.ns,
            SECURITY_GROUPS.ns,
            Keys.Namespace.SERVER_GROUPS.ns,
            Keys.Namespace.NAMED_IMAGES.ns,
            Keys.Namespace.IMAGES.ns,
            Keys.Namespace.NETWORKS.ns);

    this.defaultCaches = new HashSet<>();
    this.defaultCaches.addAll(nsList);

    this.urlMappingTemplates = new HashMap<>();
    this.urlMappingTemplates.put(
        SERVER_GROUPS.ns,
        "/applications/${application.toLowerCase()}/clusters/$account/$cluster/$provider/serverGroups/$serverGroup?region=$region");
    this.urlMappingTemplates.put(LOAD_BALANCERS.ns, "/$provider/loadBalancers/$loadBalancer");
    this.urlMappingTemplates.put(
        CLUSTERS.ns, "/applications/${application.toLowerCase()}/clusters/$account/$cluster");
    this.urlMappingTemplates.put(
        SECURITY_GROUPS.ns, "/securityGroups/$account/$provider/$region/$name");
    this.searchResultHydrators = new HashMap<>();
  }

  @Override
  public Set<String> getDefaultCaches() {
    return defaultCaches;
  }

  @Override
  public Map<String, String> getUrlMappingTemplates() {
    return urlMappingTemplates;
  }

  @Override
  public Map<SearchableResource, SearchResultHydrator> getSearchResultHydrators() {
    return searchResultHydrators;
  }

  @Override
  public Map<String, String> parseKey(String key) {
    return Keys.parse(key);
  }

  @Override
  public String getProviderName() {
    return providerName;
  }

  @Override
  public Collection<Agent> getAgents() {
    return agents;
  }
}
