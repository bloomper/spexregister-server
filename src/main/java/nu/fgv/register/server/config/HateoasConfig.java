package nu.fgv.register.server.config;

import nu.fgv.register.server.spexare.SpexareDto;
import nu.fgv.register.server.util.search.PagedWithFacetsResourcesAssembler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.util.Lazy;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;

@Configuration
public class HateoasConfig {

    private final Lazy<HateoasPageableHandlerMethodArgumentResolver> pageableResolver;

    public HateoasConfig(final ApplicationContext context) {
        this.pageableResolver = Lazy.of(() -> context.getBean("pageableResolver", HateoasPageableHandlerMethodArgumentResolver.class));
    }

    @Bean
    public PagedWithFacetsResourcesAssembler<SpexareDto> pagedWithFacetsResourcesAssembler() {
        return new PagedWithFacetsResourcesAssembler<>(pageableResolver.get(), null);
    }
}
