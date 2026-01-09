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

package nu.fgv.register.server.util.migration;

import net.datafaker.idnumbers.SwedenIdNumber;
import net.datafaker.providers.base.BaseProviders;
import net.datafaker.providers.base.IdNumber;
import net.datafaker.providers.base.PersonIdNumber;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class CustomSwedenIdNumber extends SwedenIdNumber {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public PersonIdNumber generateValid(final BaseProviders faker, final IdNumber.IdNumberRequest request) {
        final LocalDate birthday = birthday(faker, request);
        final String end = "%03d".formatted(faker.number().numberBetween(1, 1000));
        final String formattedBirthday = DATE_TIME_FORMATTER.format(birthday);
        final String basePart = formattedBirthday + "-" + end;
        final String idNumber = basePart + calculateChecksum(basePart);
        return new PersonIdNumber(idNumber, birthday, gender(faker, request));
    }

    private static LocalDate birthday(final BaseProviders faker, final IdNumber.IdNumberRequest request) {
        return faker.timeAndDate().birthday(request.minAge(), request.maxAge());
    }

    private static PersonIdNumber.Gender gender(final BaseProviders faker, final IdNumber.IdNumberRequest request) {
        final IdNumber.GenderRequest gender = request.gender();
        final PersonIdNumber.Gender result;

        switch (gender) {
            case FEMALE -> result = PersonIdNumber.Gender.FEMALE;
            case MALE -> result = PersonIdNumber.Gender.MALE;
            case ANY -> result = randomGender(faker);
            default -> throw new IncompatibleClassChangeError();
        }

        return result;
    }

    static PersonIdNumber.Gender randomGender(final BaseProviders faker) {
        return faker.bool().bool() ? PersonIdNumber.Gender.FEMALE : PersonIdNumber.Gender.MALE;
    }

    private static int calculateChecksum(final String number) {
        final String dateString = number.substring(2, 8);
        final String birthNumber = number.substring(9, 12);
        final String calculatedNumber = calculateDigits(dateString + birthNumber);
        final int sum = calculateDigitSum(calculatedNumber);
        final int lastDigit = sum % 10;
        final int difference = 10 - lastDigit;

        return difference % 10;
    }

    private static String calculateDigits(final String numbers) {
        final StringBuilder calculatedNumbers = new StringBuilder();

        for(int i = 0; i < 9; ++i) {
            final int n = numbers.charAt(i) - 48;
            final int res;

            if (i % 2 == 0) {
                res = n << 1;
            } else {
                res = n;
            }

            calculatedNumbers.append(res);
        }

        return calculatedNumbers.toString();
    }

    private static int calculateDigitSum(final String numbers) {
        int sum = 0;
        final int length = numbers.length();

        for(int i = 0; i < length; ++i) {
            final int n = numbers.charAt(i) - 48;

            sum += n;
        }

        return sum;
    }
}
