package nu.fgv.register.server.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.Spexare;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@Profile("local")
public class StartupEvent implements ApplicationListener<ApplicationReadyEvent> {

    private final IndexingService indexingService;

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        indexingService.initiateIndexingFor(Spexare.class, Boolean.parseBoolean(System.getenv("spexregister-insert-sample-data")));
    }
}