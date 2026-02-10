package iuh.fit.se.serviceidentity.dto.response;

import lombok.*;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private int currentPage;
    private int totalPages;
    private int pageSize;
    private long totalElements;

    @Builder.Default
    private List<T> data = Collections.emptyList();
}