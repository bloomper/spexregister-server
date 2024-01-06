package nu.fgv.register.server.util;

import org.springframework.http.MediaType;

public class Constants {

    private Constants() {
    }

    public static class MediaTypes {

        private MediaTypes() {
        }

        public static final String APPLICATION_XLSX_VALUE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        public static final MediaType APPLICATION_XLSX = MediaType.parseMediaType(APPLICATION_XLSX_VALUE);

        public static final String APPLICATION_XLS_VALUE = "application/vnd.ms-excel";

        public static final MediaType APPLICATION_XLS = MediaType.parseMediaType(APPLICATION_XLS_VALUE);

    }
}
