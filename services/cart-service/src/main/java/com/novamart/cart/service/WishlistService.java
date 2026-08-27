package com.novamart.cart.service;

import com.novamart.cart.client.CatalogueGateway;
import com.novamart.cart.domain.WishlistItem;
import com.novamart.cart.dto.CartDtos.ProductSnapshot;
import com.novamart.cart.dto.CartDtos.StockSnapshot;
import com.novamart.cart.dto.WishlistDtos.WishlistItemResponse;
import com.novamart.cart.dto.WishlistDtos.WishlistResponse;
import com.novamart.cart.repository.WishlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WishlistService {

    private static final Logger log = LoggerFactory.getLogger(WishlistService.class);

    private final WishlistRepository wishlistRepository;
    private final CartService cartService;
    private final CatalogueGateway catalogue;

    public WishlistService(WishlistRepository wishlistRepository, CartService cartService, CatalogueGateway catalogue) {
        this.wishlistRepository = wishlistRepository;
        this.cartService = cartService;
        this.catalogue = catalogue;
    }

    @Transactional(readOnly = true)
    public WishlistResponse getWishlist(UUID userId) {
        List<WishlistItem> items = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return present(userId, items);
    }

    @Transactional
    public WishlistResponse addItem(UUID userId, UUID productId) {
        // Validate product exists in catalogue
        ProductSnapshot product = catalogue.requireProduct(productId);

        if (!wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            WishlistItem item = WishlistItem.create(userId, productId);
            wishlistRepository.save(item);
            log.info("Added product {} ({}) to wishlist for user {}", product.name(), productId, userId);
        }
        return getWishlist(userId);
    }

    @Transactional
    public WishlistResponse removeItem(UUID userId, UUID productId) {
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
        log.info("Removed product {} from wishlist for user {}", productId, userId);
        return getWishlist(userId);
    }

    @Transactional
    public void moveToCart(UUID userId, UUID productId) {
        // Add 1 quantity to cart
        cartService.addItem(userId, productId, 1);
        // Remove from wishlist
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
        log.info("Moved product {} to cart for user {}", productId, userId);
    }

    private WishlistResponse present(UUID userId, List<WishlistItem> items) {
        if (items.isEmpty()) {
            return new WishlistResponse(userId, List.of(), 0);
        }

        List<UUID> productIds = items.stream().map(WishlistItem::getProductId).toList();
        Map<UUID, ProductSnapshot> products = catalogue.productsByIds(productIds);
        Map<UUID, StockSnapshot> stock = catalogue.stockByIds(productIds);

        List<WishlistItemResponse> lines = new ArrayList<>();
        List<WishlistItem> vanished = new ArrayList<>();

        for (WishlistItem item : items) {
            ProductSnapshot product = products.get(item.getProductId());
            if (product == null) {
                vanished.add(item);
                continue;
            }
            StockSnapshot snapshot = stock.get(item.getProductId());
            lines.add(new WishlistItemResponse(
                    item.getId(),
                    product.id(),
                    product.name(),
                    product.slug(),
                    product.imageUrl(),
                    product.price(),
                    product.currency(),
                    snapshot == null ? 0 : snapshot.availableQuantity(),
                    snapshot != null && snapshot.inStock(),
                    item.getCreatedAt()));
        }

        if (!vanished.isEmpty()) {
            wishlistRepository.deleteAll(vanished);
        }

        return new WishlistResponse(userId, lines, lines.size());
    }
}
