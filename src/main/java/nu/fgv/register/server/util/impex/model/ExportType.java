package nu.fgv.register.server.util.impex.model;

import lombok.Getter;

@Getter
public enum ExportType {
    PDF_ADDRESS_LABELS("pdf-address-labels"),
    PDF_SCRATCH_LIST("pdf-scratch-list"),
    PDF_PLATOON_LIST("pdf-platoon-list");

    private final String value;

    ExportType(final String value) {
        this.value = value;
    }

    public static ExportType fromValue(final String value) {
        for (final ExportType type : ExportType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown export type: " + value);
    }
}
