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

package nu.fgv.register.server.util.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
public class LuhnValidator implements ConstraintValidator<Luhn, String> {

    private Pattern pattern;
    private int[] inputGroups;
    private int controlGroup;
    private int existenceGroup;

    public LuhnValidator() {
        pattern = Pattern.compile("");
        inputGroups = new int[0];
    }

    @Override
    public void initialize(final Luhn parameters) {
        try {
            pattern = Pattern.compile(parameters.regexp());
        } catch (final PatternSyntaxException e) {
            log.error("Invalid pattern {} specified", parameters.regexp());
            throw e;
        }
        this.inputGroups = parameters.inputGroups();
        this.controlGroup = parameters.controlGroup();
        this.existenceGroup = parameters.existenceGroup();
    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (hasText(value)) {
            final Matcher matches = pattern.matcher(value);

            if (!matches.find()) {
                return false;
            }

            final String lastPart = matches.group(existenceGroup);
            if (!hasText(lastPart)) {
                return true;
            }

            final StringBuilder sb = new StringBuilder();
            Arrays.stream(inputGroups).forEach(inputGroup -> sb.append(matches.group(inputGroup)));
            final String controlNumber = matches.group(controlGroup);

            return luhn(sb.toString()) == Integer.parseInt(controlNumber);
        } else {
            return true;
        }
    }

    private int luhn(final String value) {
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
