package com.novamart.cart.service;

import com.novamart.cart.client.CatalogueGateway;
import com.novamart.cart.domain.Cart;
import com.novamart.cart.domain.CartItem;
import com.novamart.cart.dto.CartDtos.CartItemResponse;
import com.novamart.cart.dto.CartDtos.CartResponse;
import com.novamart.cart.dto.CartDtos.ProductSnapshot;
import com.novamart.cart.dto.CartDtos.StockSnapshot;
import com.novamart.cart.repository.CartRepository;
import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartRepository carts;
    private final CatalogueGateway catalogue;

    public CartService(CartRepository carts, CatalogueGateway catalogue) {
        this.carts = carts;
        this.catalogue = catalogue;
    }

    @Transactional
    public CartResponse getOrCreate(UUID userId) {
        return present(loadOrCreate(userId));
    }

    @Transactional
    public CartResponse addItem(UUID userId, UUID productId, int quantity) {
        // Confirms the product exists and is active before a line is written.
        // Without this a typo, or a product deleted between page load and click,
        // would leave an unresolvable line in the cart that renders as blank.
        ProductSnapshot product = catalogue.requireProduct(productId);

        Cart cart = loadOrCreate(userId);
        int alreadyInCart = cart.findItem(productId).map(CartItem::getQuantity).orElse(0);
        assertStock(productId, alreadyInCart + quantity);

        cart.addOrIncrement(productId, quantity);
        log.info("Added {} x {} to cart for user {}", quantity, product.name(), userId);
        return present(cart);
    }

    @Transactional
    public CartResponse updateItem(UUID userId, UUID productId, int quantity) {
        Cart cart = loadOrCreate(userId);
        if (cart.findItem(productId).isEmpty()) {
            throw new ApiException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        if (quantity > 0) {
            assertStock(productId, quantity);
        }
        cart.setQuantity(productId, quantity);
        return present(cart);
    }

    @Transactional
    public CartResponse removeItem(UUID userId, UUID productId) {
        Cart cart = loadOrCreate(userId);
        if (!cart.remove(productId)) {
            throw new ApiException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        return present(cart);
    }

    @Transactional
    public void clear(UUID userId) {
        carts.findByUserId(userId).ifPresent(Cart::clear);
    }

    private Cart loadOrCreate(UUID userId) {
        // An empty cart is a normal state, not a 404. Returning an error for a
        // shopper who has never added anything would force every caller to
        // special-case it.
        return carts.findByUserId(userId).orElseGet(() -> carts.save(Cart.forUser(userId)));
    }

    private void assertStock(UUID productId, int requestedTotal) {
        int available = catalogue.availableQuantity(productId);
        if (available < requestedTotal) {
            throw new ApiException(ErrorCode.INSUFFICIENT_STOCK,
                    available == 0
                            ? "This item is out of stock"
                            : "Only " + available + " units of this item are available");
        }
    }

    /**
     * Merges live product and stock data into the stored lines.
     *
     * <p>A line whose product has since been deleted is dropped from the response
     * rather than rendered blank, and the stored line is removed so the cart
     * self-heals instead of accumulating rubbish.
     */
    private CartResponse present(Cart cart) {
        List<UUID> ids = cart.getItems().stream().map(CartItem::getProductId).toList();
        Map<UUID, ProductSnapshot> products = ids.isEmpty() ? Map.of() : catalogue.productsByIds(ids);
        Map<UUID, StockSnapshot> stock = ids.isEmpty() ? Map.of() : catalogue.stockByIds(ids);

        List<CartItemResponse> lines = new ArrayList<>();
        List<UUID> vanished = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalQuantity = 0;
        String currency = "INR";

        for (CartItem item : cart.getItems()) {
            ProductSnapshot product = products.get(item.getProductId());
            if (product == null) {
                vanished.add(item.getProductId());
                continue;
            }
            StockSnapshot snapshot = stock.get(item.getProductId());
            BigDecimal lineTotal = product.price().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);
            totalQuantity += item.getQuantity();
            currency = product.currency();

            lines.add(new CartItemResponse(
                    product.id(), product.name(), product.slug(), product.imageUrl(),
                    product.price(), item.getQuantity(), lineTotal,
                    snapshot == null ? 0 : snapshot.availableQuantity(),
                    snapshot != null && snapshot.inStock()));
        }

        if (!vanished.isEmpty()) {
            log.info("Dropping {} cart line(s) for products that no longer exist", vanished.size());
            vanished.forEach(cart::remove);
        }

        return new CartResponse(cart.getId(), cart.getUserId(), lines,
                subtotal, totalQuantity, currency, cart.getUpdatedAt());
    }
}
