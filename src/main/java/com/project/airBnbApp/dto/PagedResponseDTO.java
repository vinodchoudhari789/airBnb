package com.project.airBnbApp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Generic paginated response wrapper.
 *
 * Shaped to match DevExtreme's CustomStore.load() expected result -
 * { data: [...], totalCount: N } - so it can be returned directly from a
 * CustomStore's load function without remapping on the frontend:
 *
 *   load(loadOptions) {
 *     return fetch(`/admin/hotels/${hotelId}/bookings?skip=${loadOptions.skip}&take=${loadOptions.take}`)
 *       .then(r => r.json()); // already { data, totalCount }
 *   }
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagedResponseDTO<T> {
    private List<T> data;
    private long totalCount;

    public static <S, T> PagedResponseDTO<T> from(Page<S> page, Function<S, T> mapper) {
        return new PagedResponseDTO<>(page.getContent().stream().map(mapper).toList(), page.getTotalElements());
    }
}
