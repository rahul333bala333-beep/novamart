package com.novamart.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Ids to resolve in one call.
 *
 * <p>Capped at 100 so a caller cannot turn a single request into an unbounded
 * scan of the stock table.
 */
public record BatchLookupRequest(
        @NotNull @Size(min = 1, max = 100) List<UUID> productIds) {
}
