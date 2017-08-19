package workflowSuite.kpiClient.settings

interface IConfigurationProvider<T> {
    fun TryGetValidConfiguration() : GetConfigurationResult<T>
}

data class GetConfigurationResult<T>(val success: Boolean, val configuration: T)