package tn.lim3.driveeasy.location.dto;

import java.util.List;

/** Page de resultats (pagination en memoire). */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
}
