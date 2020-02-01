package nu.fgv.register.server.config

import org.springframework.context.annotation.Configuration
import org.springframework.hateoas.config.EnableHypermediaSupport
import org.springframework.hateoas.support.WebStack

@Configuration
@EnableHypermediaSupport(type = [EnableHypermediaSupport.HypermediaType.HAL], stacks = [WebStack.WEBMVC])
class HypermediaConfig
