package nu.fgv.register.server.util.export.model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelCell {

    int position();

    String header() default "";

    String transform() default "";

    boolean crossRef() default false;

    int crossRefPosition() default -1;

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Exclude {}
}