package nu.fgv.register.server.util.randomizer;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import org.jeasy.random.api.Randomizer;

import java.util.Locale;

public class SocialSecurityNumberRandomizer implements Randomizer<String> {

    private final FakeValuesService fakeValuesService = new FakeValuesService(Locale.ENGLISH, new RandomService());

    @Override
    public String getRandomValue() {
        return fakeValuesService.regexify("\\d{4}");
    }
}
