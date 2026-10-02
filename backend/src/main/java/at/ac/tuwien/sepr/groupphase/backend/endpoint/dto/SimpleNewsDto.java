package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

public class SimpleNewsDto {


    private Long id;

    private LocalDateTime publishedAt;

    private String title;

    private String summary;

    private byte[] image;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

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

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SimpleNewsDto that)) {
            return false;
        }
        return Objects.equals(id, that.id)
            && Objects.equals(publishedAt, that.publishedAt)
            && Objects.equals(title, that.title)
            && Objects.equals(summary, that.summary)
            && Arrays.equals(image, that.image);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, publishedAt, title, summary, Arrays.hashCode(image));
    }

    @Override
    public String toString() {
        return "SimpleNewsDto{"
            + "id=" + id
            + ", publishedAt=" + publishedAt
            + ", title='" + title + '\''
            + ", summary='" + summary + '\''
            + ", image=" + Arrays.toString(image)
            + '}';
    }


    public static final class SimpleNewsDtoBuilder {
        private Long id;
        private LocalDateTime publishedAt;
        private String title;
        private String summary;
        private byte[] image;

        private SimpleNewsDtoBuilder() {
        }

        public static SimpleNewsDtoBuilder aSimpleNewsDto() {
            return new SimpleNewsDtoBuilder();
        }

        public SimpleNewsDtoBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public SimpleNewsDtoBuilder withPublishedAt(LocalDateTime publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public SimpleNewsDtoBuilder withTitle(String title) {
            this.title = title;
            return this;
        }

        public SimpleNewsDtoBuilder withSummary(String summary) {
            this.summary = summary;
            return this;
        }

        public SimpleNewsDtoBuilder withImage(byte[] image) {
            this.image = image;
            return this;
        }

        public SimpleNewsDto build() {
            SimpleNewsDto simpleNewsDto = new SimpleNewsDto();
            simpleNewsDto.setId(id);
            simpleNewsDto.setPublishedAt(publishedAt);
            simpleNewsDto.setTitle(title);
            simpleNewsDto.setSummary(summary);
            simpleNewsDto.setImage(image);
            return simpleNewsDto;
        }
    }
}
