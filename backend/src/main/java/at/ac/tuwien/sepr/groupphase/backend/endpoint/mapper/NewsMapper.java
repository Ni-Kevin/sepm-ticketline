package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.DetailedNewsDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.NewsInquiryDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.SimpleNewsDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.News;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NewsMapper {

    @Named("simpleNews")
    SimpleNewsDto newsToSimpleNewsDto(News news);

    /**
     * This is necessary since the SimpleNewsDto misses the text property and the collection mapper can't handle
     * missing fields.
     **/
    @IterableMapping(qualifiedByName = "simpleNews")
    List<SimpleNewsDto> newsToSimpleNewsDto(List<News> news);

    DetailedNewsDto newsToDetailedNewsDto(News news);

    News detailedNewsDtoToNews(DetailedNewsDto detailedNewsDto);

    News newsInquiryDtoToNews(NewsInquiryDto newsInquiryDto);

    NewsInquiryDto newsToNewsInquiryDto(News news);
}
