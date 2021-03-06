package workflowsuite.kpi.client.serviceregistry;

import java.util.ArrayList;

public final class ServiceEndpointsInfo {
    public static final ServiceEndpointsInfo EMPTY = new ServiceEndpointsInfo();

    public String deploymentUnitName = "";

    public String serviceKind = "";

    public final ArrayList<EndpointConfiguration> endpoints = new ArrayList<>(0);

    public final ArrayList<TransportSettings> transportSettigs = new ArrayList<>(0);
}
