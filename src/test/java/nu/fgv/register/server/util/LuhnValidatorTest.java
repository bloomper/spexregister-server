package nu.fgv.register.server.util;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import nu.fgv.register.server.spexare.Spexare;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.Annotation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(MockitoExtension.class)
class LuhnValidatorTest {

    private final LuhnValidator validator = new LuhnValidator();

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @BeforeEach
    public void setUp() {
        final SocialSecurityNumberTestClass testClass = new SocialSecurityNumberTestClass();

        validator.initialize(testClass);
    }

    @Test
    void should_return_true_if_correct_social_security_number() {
        assertThat(validator.isValid("20120606-4659", constraintValidatorContext), is(true));
    }

    @Test
    void should_return_false_if_incorrect_social_security_number() {
        assertThat(validator.isValid("20120606-4658", constraintValidatorContext), is(false));
    }

    @Test
    void should_return_true_if_last_part_of_social_security_number_is_missing() {
        assertThat(validator.isValid("20120606", constraintValidatorContext), is(true));
    }

    private static class SocialSecurityNumberTestClass implements Luhn {

        @Override
        public String regexp() {
            return Spexare.SOCIAL_SECURITY_NUMBER_PATTERN;
        }

        @Override
        public int[] inputGroups() {
            return new int[]{2, 3, 6, 11};
        }

        @Override
        public int controlGroup() {
            return 12;
        }

        @Override
        public int existenceGroup() {
            return 10;
        }

        @Override
        public String message() {
            return "Invalid control number";
        }

        @Override
        public Class<?>[] groups() {
            return new Class[]{};
        }

        @Override
        public Class<? extends Payload>[] payload() {
            return new Class[0];
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }
    }
}