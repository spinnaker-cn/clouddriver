package com.netflix.spinnaker.clouddriver.ecloud.deploy;

import com.netflix.frigga.Names;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudServerGroup;
import com.netflix.spinnaker.clouddriver.ecloud.provider.view.EcloudClusterProvider;
import com.netflix.spinnaker.clouddriver.helpers.AbstractServerGroupNameResolver;
import com.netflix.spinnaker.clouddriver.model.Cluster;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class EcloudServerGroupNameResolver extends AbstractServerGroupNameResolver {

  private static final String ECLOUD_PHASE = "ECLOUD_DEPLOY";

  private final String accountName;
  private final String region;
  private final EcloudClusterProvider clusterProvider;

  public EcloudServerGroupNameResolver(
      String accountName, String region, EcloudClusterProvider clusterProvider) {
    this.accountName = accountName;
    this.region = region;
    this.clusterProvider = clusterProvider;
  }

  @Override
  public String getPhase() {
    return ECLOUD_PHASE;
  }

  @Override
  public String getRegion() {
    return region;
  }

  @Override
  public List<TakenSlot> getTakenSlots(String clusterName) {
    String app = Names.parseName(clusterName).getApp();
    Cluster cluster = clusterProvider.getCluster(app, accountName, clusterName);
    if (cluster == null) {
      return Collections.emptyList();
    }
    ArrayList<EcloudServerGroup> serverGroups =
        (ArrayList<EcloudServerGroup>)
            cluster.getServerGroups().stream()
                .filter(it -> region.equals(it.getRegion()))
                .collect(Collectors.toList());
    List<TakenSlot> takenSlots = new ArrayList<>();
    serverGroups.stream()
        .forEach(
            it -> {
              takenSlots.add(
                  new TakenSlot(
                      it.getName(),
                      Names.parseName(it.getName()).getSequence(),
                      new Date(it.getCreatedTime())));
            });
    return takenSlots;
  }
}
