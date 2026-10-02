package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Objects;

public class NewsInquiryDto {

    @NotBlank(message = "Title must not be blank")
    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String title;

    @NotBlank(message = "Summary must not be blank")
    @Size(max = 250, message = "Summary must not exceed 250 characters")
    private String summary;

    @Size(max = 10000, message = "Text must not exceed 10000 characters")
    private String text;

    private LocalDateTime publishedAt;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NewsInquiryDto that)) {
            return false;
        }
        return Objects.equals(title, that.title)
            && Objects.equals(summary, that.summary)
            && Objects.equals(text, that.text)
            && Objects.equals(publishedAt, that.publishedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, summary, text, publishedAt);
    }

    @Override
    public String toString() {
        return "NewsInquiryDto{"
            + "title='" + title + '\''
            + ", summary='" + summary + '\''
            + ", text='" + text + '\''
            + ", publishedAt=" + publishedAt
            + '}';
    }


    public static final class NewsInquiryDtoBuilder {
        private String title;
        private String summary;
        private String text;
        private LocalDateTime publishedAt;

        private NewsInquiryDtoBuilder() {
        }

        public static NewsInquiryDtoBuilder aNewsInquiryDto() {
            return new NewsInquiryDtoBuilder();
        }

        public NewsInquiryDtoBuilder withTitle(String title) {
            this.title = title;
            return this;
        }

        public NewsInquiryDtoBuilder withSummary(String summary) {
            this.summary = summary;
            return this;
        }

        public NewsInquiryDtoBuilder withText(String text) {
            this.text = text;
            return this;
        }

        public NewsInquiryDtoBuilder withPublishedAt(LocalDateTime publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public NewsInquiryDto build() {
            NewsInquiryDto newsInquiryDto = new NewsInquiryDto();
            newsInquiryDto.setTitle(title);
            newsInquiryDto.setSummary(summary);
            newsInquiryDto.setText(text);
            newsInquiryDto.setPublishedAt(publishedAt);
            return newsInquiryDto;
        }
    }
}
