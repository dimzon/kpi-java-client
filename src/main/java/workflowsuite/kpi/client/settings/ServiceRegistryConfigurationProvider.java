package workflowsuite.kpi.client.settings;

import java.time.Duration;
import java.time.Instant;

import workflowsuite.kpi.client.serviceregistry.ServiceRegistryClient;

public abstract class ServiceRegistryConfigurationProvider<T> implements ConfigurationProvider<T> {

    private final Duration refreshPeriod;
    private Instant lastSync;
    protected final ServiceRegistryClient serviceRegistryClient;
    protected T configuration;

    /**
     * Create instance of {{@link ServiceRegistryClient}} class.
     * @param serviceRegistryClient The instance of service registry client, which use for read configuration.
     * @param refreshPeriod The period after which the settings are read again
     */
    public ServiceRegistryConfigurationProvider(ServiceRegistryClient serviceRegistryClient,
                                                Duration refreshPeriod) {
        this.serviceRegistryClient = serviceRegistryClient;
        this.refreshPeriod = refreshPeriod;
        this.configuration = null;
        this.lastSync = Instant.MIN;
    }

    /**
     * Read configuration from service registry. Do sync refresh after period.
     * @return Configuration.
     */
    @Override
    public final GetConfigurationResult<T> tryGetValidConfiguration() {
        if (Duration.between(lastSync, Instant.now()).compareTo(refreshPeriod) > 0) {
            syncConfiguration();
            lastSync = Instant.now();
        }

        if (configuration != null) {
            return GetConfigurationResult.<T>success(configuration);
        }

        return GetConfigurationResult.<T>fail();
    }

    protected abstract void syncConfiguration();
}
