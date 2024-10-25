package nu.fgv.register.server.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.Spexare;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class StartupEvent implements ApplicationListener<ApplicationReadyEvent> {

    private final IndexingService indexingService;

    @Value("${spexregister.sample-data.import:false}")
    private boolean importSampleData;

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        indexingService.initiateIndexingFor(Spexare.class, importSampleData);
    }
}