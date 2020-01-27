package nu.fgv.register.server.config

import org.elasticsearch.client.RestHighLevelClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.RestClients
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories

@Configuration
@EnableElasticsearchRepositories
class SearchConfig {

    @Bean
    fun elasticsearchClient(): RestHighLevelClient {
        val configuration = ClientConfiguration.localhost()
        return RestClients.create(configuration).rest()
    }

    @Bean
    fun elasticsearchTemplate(): ElasticsearchRestTemplate {
        return ElasticsearchRestTemplate(elasticsearchClient())
    }
}
