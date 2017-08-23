package workflowsuite.kpi.client.serviceregistry;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.time.Duration;

public final class ServiceRegistryClient {
    private static final String LOCALHOST = "localhost";
    private static final String SERVICE_REGISTRY_BASE_RESOURCE = "worklfow/serviceregistry/api/v1/";

    public static final Duration DEFAULT_REFRESH_TIME = Duration.ofSeconds(5);


    private final String serverEndpoint;
    private final String clientName;

    public ServiceRegistryClient(URI serverEndpoint) {

        this.serverEndpoint = serverEndpoint.toString();
        this.clientName = getMachineName();
    }

    public final ServiceEndpointsInfo getServiceEndpointsInfo(String contract) {

        HttpURLConnection connection = null;
        String query = this.serverEndpoint + SERVICE_REGISTRY_BASE_RESOURCE + "serviceendpoints?contract=" +
                contract + "&client="+ this.clientName;
        try {
            URL url = new URL(query);
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/xml; charset=utf-8");
            connection.setRequestProperty("Accept", "application/xml");
            connection.setRequestProperty("User-Agent", "Workflow Suite KPI Java Client");
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                ServiceEndpointInfosXmlParser serviceInfosParser = new ServiceEndpointInfosXmlParser();
                try(InputStream inputStream = connection.getInputStream()) {
                    return serviceInfosParser.parse(inputStream);
                }
            }
        } catch (IOException ex) {
            return ServiceEndpointsInfo.Empty;
        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
        }


        return ServiceEndpointsInfo.Empty;

    }

    private static String getMachineName()
    {
        try
        {
            InetAddress address = InetAddress.getLocalHost();
            return address.getHostName();
        }
        catch (Exception ex)
        {
            return LOCALHOST;
        }
    }
}