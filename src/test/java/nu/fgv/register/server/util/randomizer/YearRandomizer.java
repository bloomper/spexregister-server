package nu.fgv.register.server.util.randomizer;

import net.datafaker.service.FakeValuesService;
import net.datafaker.service.RandomService;
import org.jeasy.random.api.Randomizer;

import java.util.Locale;

public class YearRandomizer implements Randomizer<String> {

    private final FakeValuesService fakeValuesService = new FakeValuesService(Locale.ENGLISH, new RandomService());

    @Override
    public String getRandomValue() {
        return fakeValuesService.regexify("(19|20|21)\\d{2}");
    }
}
