package nu.fgv.register.server.user.authority;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Set;

@Mapper(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
@MapperConfig(
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR
)
public interface AuthorityMapper {

    AuthorityMapper AUTHORITY_MAPPER = Mappers.getMapper(AuthorityMapper.class);

    @Mapping(target = "label", ignore = true)
    AuthorityDto toDto(Authority model);

    Set<AuthorityDto> toDtos(Set<Authority> models);

    @AfterMapping
    default void setLabel(final Authority model, final @MappingTarget AuthorityDto.AuthorityDtoBuilder dto) {
        dto.label(model.getLabels().get(LocaleContextHolder.getLocale().getLanguage()));
    }

}
