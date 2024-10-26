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

package nu.fgv.register.server.util.impex.importing;

import nu.fgv.register.server.util.Constants;
import nu.fgv.register.server.util.impex.model.ImportResultDto;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public abstract class AbstractImportService {

    public ImportResultDto doImport(final byte[] file, final String type, final Locale locale) throws IOException {
        try (final var workbook = convertByteArrayToWorkbook(file, type)) {
            final var validationResult = doValidate(workbook, locale);

            return validationResult.isSuccess() ? doImport(workbook, locale) : validationResult;
        }
    }

    protected abstract ImportResultDto doImport(final Workbook workbook, final Locale locale);

    protected abstract ImportResultDto doValidate(final Workbook workbook, final Locale locale);

    private Workbook convertByteArrayToWorkbook(final byte[] file, final String type) throws IOException {
        final var inputStream = new ByteArrayInputStream(file);
        switch (type) {
            case Constants.MediaTypes.APPLICATION_XLSX_VALUE -> {
                return new XSSFWorkbook(inputStream);
            }
            case Constants.MediaTypes.APPLICATION_XLS_VALUE -> {
                return new HSSFWorkbook(inputStream);
            }
            default -> throw new IllegalArgumentException("Unrecognized type");
        }
    }

}
