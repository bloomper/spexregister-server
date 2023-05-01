package nu.fgv.register.server.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;

@Target( { FIELD, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = LuhnValidator.class)
public @interface Luhn {

    String regexp();
    int[] inputGroups();
    int controlGroup();
    int existenceGroup();
    String message() default "Invalid control number";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};


}
