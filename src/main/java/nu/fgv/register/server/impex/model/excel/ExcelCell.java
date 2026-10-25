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

package nu.fgv.register.server.impex.model.excel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A column of a sheet. Exported cells are colored and locked by whether the column is updatable and mandatory.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelCell {

    /**
     * @return the column, from zero; audit columns are placed after the others
     */
    int position();

    /**
     * @return whether the column identifies the record
     */
    boolean primaryKey() default false;

    /**
     * @return whether an imported row must have a value
     */
    boolean mandatory() default false;

    /**
     * @return whether an import may change the value, and the exported cell is unlocked
     */
    boolean updatable() default false;

    /**
     * @return whether the column is left out of an export where no row has a value
     */
    boolean ignoreIfNull() default false;

    /**
     * @return the message key of the header
     */
    String header() default "";

    /**
     * @return a SpEL expression applied to the value when it is exported, or blank for none
     */
    String transform() default "";

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Exclude {
    }
}