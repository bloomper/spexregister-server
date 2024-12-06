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
import org.jeasy.random.api.Randomizer;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class LabelsRandomizer implements Randomizer<Map<String, String>> {

    private static final String[] LANGUAGES = new String[]{"sv", "en"};
    private final FakeValuesService fakeValuesService = new FakeValuesService();
    private final FakerContext fakerContext = new FakerContext(Locale.ENGLISH, new RandomService());

    @Override
    public Map<String, String> getRandomValue() {
        return Stream.of(LANGUAGES)
                .collect(Collectors.toMap(
                        language -> language,
                        this::generateRandomString
                ));
    }

    private String generateRandomString(final String language) {
        return fakeValuesService.regexify("[A-Za-z]{10}", fakerContext);
    }
}
