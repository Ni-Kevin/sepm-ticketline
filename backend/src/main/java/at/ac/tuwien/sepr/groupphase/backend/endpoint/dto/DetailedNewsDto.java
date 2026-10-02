package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import java.time.LocalDateTime;
import java.util.Objects;

public class DetailedNewsDto extends SimpleNewsDto {

    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DetailedNewsDto that)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        return Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), text);
    }

    @Override
    public String toString() {
        return "DetailedNewsDto{"
            + "text='" + text + '\''
            + '}';
    }


    public static final class DetailedNewsDtoBuilder {
        private Long id;
        private LocalDateTime publishedAt;
        private String text;
        private String title;
        private String summary;

        private DetailedNewsDtoBuilder() {
        }

        public static DetailedNewsDtoBuilder aDetailedNewsDto() {
            return new DetailedNewsDtoBuilder();
        }

        public DetailedNewsDtoBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public DetailedNewsDtoBuilder withPublishedAt(LocalDateTime publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public DetailedNewsDtoBuilder withText(String text) {
            this.text = text;
            return this;
        }

        public DetailedNewsDtoBuilder withTitle(String title) {
            this.title = title;
            return this;
        }

        public DetailedNewsDtoBuilder withSummary(String summary) {
            this.summary = summary;
            return this;
        }

        public DetailedNewsDto build() {
            DetailedNewsDto detailedNewsDto = new DetailedNewsDto();
            detailedNewsDto.setId(id);
            detailedNewsDto.setPublishedAt(publishedAt);
            detailedNewsDto.setText(text);
            detailedNewsDto.setTitle(title);
            detailedNewsDto.setSummary(summary);
            return detailedNewsDto;
        }
    }
}
