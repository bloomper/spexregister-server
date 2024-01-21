package nu.fgv.register.server.util.randomizer;

import net.datafaker.service.FakeValuesService;
import net.datafaker.service.FakerContext;
import net.datafaker.service.RandomService;
import nu.fgv.register.server.spexare.Spexare;
import org.jeasy.random.api.Randomizer;

import java.util.Locale;

public class SocialSecurityNumberRandomizer implements Randomizer<String> {

    private final FakeValuesService fakeValuesService = new FakeValuesService();
    private final FakerContext fakerContext = new FakerContext(Locale.ENGLISH, new RandomService());

    @Override
    public String getRandomValue() {
        final String socialSecurityNumber = fakeValuesService.regexify(Spexare.SOCIAL_SECURITY_NUMBER_PATTERN, fakerContext);

        if (socialSecurityNumber.length() == 8) {
            return socialSecurityNumber;
        } else {
            return socialSecurityNumber.substring(0, 12) + recalculateControlNumber(socialSecurityNumber.substring(2, 12).replaceAll("-", ""));
        }
    }

    private int recalculateControlNumber(final String value) {
        int temp;
        int sum = 0;

        for (int i = 0; i < value.length(); i++) {
            temp = Character.getNumericValue(value.charAt(i));
            temp *= 2 - (i % 2);
            if (temp > 9)
                temp -= 9;

            sum += temp;
        }

        return (10 - (sum % 10)) % 10;
    }
}
