package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import java.util.List;

public class NewsPageDto {

    private List<SimpleNewsDto> content;
    private int totalPages;
    private long totalElements;

    public NewsPageDto() {
    }

    public NewsPageDto(List<SimpleNewsDto> content, int totalPages, long totalElements) {
        this.content = content;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
    }

    public List<SimpleNewsDto> getContent() {
        return content;
    }

    public void setContent(List<SimpleNewsDto> content) {
        this.content = content;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }
}
