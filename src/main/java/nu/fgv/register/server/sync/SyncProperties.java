package nu.fgv.register.server.sync;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where Spexipedia is, and whether to sync from it ({@code spexregister.spexipedia.*}).
 *
 * @param enabled    whether to pull Spexipedia's change feed
 * @param graphqlUrl Spexipedia's GraphQL API, e.g. {@code http://spexipedia-api:8080/api/graphql}
 * @param webUrl     Spexipedia's web, for links to its pages
 * @author Anders Jacobsson
 * @since 2.0
 */
@ConfigurationProperties(prefix = "spexregister.spexipedia")
public record SyncProperties(boolean enabled, String graphqlUrl, String webUrl) {
}
