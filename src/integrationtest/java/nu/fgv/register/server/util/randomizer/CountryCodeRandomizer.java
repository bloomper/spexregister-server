/*
 * Copyright 2026 the original author or authors.
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

import graphql.scalars.country.code.CountryCode;
import net.datafaker.service.FakerContext;
import net.datafaker.service.RandomService;
import org.jeasy.random.api.Randomizer;

import java.util.Locale;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class CountryCodeRandomizer implements Randomizer<String> {

    private final FakerContext fakerContext = new FakerContext(Locale.ENGLISH, new RandomService());

    @Override
    public String getRandomValue() {
        final CountryCode[] countryCodes = CountryCode.values();
        final int index = fakerContext.getRandomService().nextInt(countryCodes.length);
        return countryCodes[index].name();
    }
}
