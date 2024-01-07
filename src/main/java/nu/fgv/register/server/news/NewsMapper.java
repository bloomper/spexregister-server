package nu.fgv.register.server.news;

import org.mapstruct.AfterMapping;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;

@Mapper(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
@MapperConfig(
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR
)
public interface NewsMapper {

    NewsMapper NEWS_MAPPER = Mappers.getMapper(NewsMapper.class);

    NewsDto toDto(News model);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "published", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    News toModel(NewsCreateDto dto);

    @Mapping(target = "published", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    News toModel(NewsUpdateDto dto);

    @InheritConfiguration(name = "toModel")
    void toPartialModel(NewsUpdateDto dto, @MappingTarget News model);

    @AfterMapping
    default void setPublished(NewsCreateDto dto, final @MappingTarget News model) {
        model.setPublished(isPublished(dto.getVisibleFrom(), dto.getVisibleTo()));
    }

    @AfterMapping
    default void setPublished(NewsUpdateDto dto, final @MappingTarget News model) {
        model.setPublished(isPublished(dto.getVisibleFrom(), dto.getVisibleTo()));
    }

    default boolean isPublished(final LocalDate visibleFrom, final LocalDate visibleTo) {
        final LocalDate today = LocalDate.now();

        return (visibleFrom != null && (visibleFrom.isEqual(today) || visibleFrom.isBefore(today))) &&
                (visibleTo != null && (visibleTo.isEqual(today) || visibleTo.isAfter(today)));
    }
}
