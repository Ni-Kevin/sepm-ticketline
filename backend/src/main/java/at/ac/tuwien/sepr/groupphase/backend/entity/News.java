package at.ac.tuwien.sepr.groupphase.backend.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;


@Entity
@Table(name = "news")
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "published_at")
    private LocalDateTime publishedAt;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(length = 10000)
    private String text;

    @Lob
    @Column
    private byte[] image;

    @OneToMany(mappedBy = "news", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<NewsReadState> readStates = new ArrayList<>();

    public News() {
    }

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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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
        if (!(o instanceof News news)) {
            return false;
        }
        return Objects.equals(id, news.id)
            && Objects.equals(publishedAt, news.publishedAt)
            && Objects.equals(title, news.title)
            && Objects.equals(summary, news.summary)
            && Objects.equals(text, news.text)
            && Arrays.equals(image, news.image);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, publishedAt, title, summary, text, Arrays.hashCode(image));
    }

    @Override
    public String toString() {
        return "News{"
            + "id=" + id
            + ", publishedAt=" + publishedAt
            + ", title='" + title + '\''
            + ", summary='" + summary + '\''
            + ", text='" + text + '\''
            + ", image=" + Arrays.toString(image)
            + '}';
    }


    public static final class NewsBuilder {
        private Long id;
        private LocalDateTime publishedAt;
        private String title;
        private String summary;
        private String text;
        private byte[] image;

        private NewsBuilder() {
        }

        public static NewsBuilder aNews() {
            return new NewsBuilder();
        }

        public NewsBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public NewsBuilder withPublishedAt(LocalDateTime publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public NewsBuilder withTitle(String title) {
            this.title = title;
            return this;
        }

        public NewsBuilder withSummary(String summary) {
            this.summary = summary;
            return this;
        }

        public NewsBuilder withText(String text) {
            this.text = text;
            return this;
        }

        public NewsBuilder withImage(byte[] image) {
            this.image = image;
            return this;
        }

        public News build() {
            News news = new News();
            news.setId(id);
            news.setPublishedAt(publishedAt);
            news.setTitle(title);
            news.setSummary(summary);
            news.setText(text);
            news.setImage(image);
            return news;
        }
    }
}
