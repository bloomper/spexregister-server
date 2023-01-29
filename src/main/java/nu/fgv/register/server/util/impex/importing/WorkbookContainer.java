package nu.fgv.register.server.util.impex.importing;

import lombok.Getter;
import lombok.Setter;
import nu.fgv.register.server.util.impex.util.AbstractWorkbookContainer;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Getter
@Setter
class WorkbookContainer extends AbstractWorkbookContainer {
    private final List<String> messages = new ArrayList<>();
    private Class<?> createClazz;
    private Class<?> updateClazz;
    private Function<Long, Boolean> existenceChecker;
    private final Validator validator;

    WorkbookContainer() {
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }
}
