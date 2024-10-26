/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nu.fgv.register.server.util.randomizer;

import net.datafaker.service.FakeValuesService;
import net.datafaker.service.FakerContext;
import net.datafaker.service.RandomService;
import nu.fgv.register.server.spexare.Spexare;
import org.jeasy.random.api.Randomizer;

import java.util.Locale;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
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
