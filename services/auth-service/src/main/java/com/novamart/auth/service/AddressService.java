package com.novamart.auth.service;

import com.novamart.auth.domain.Address;
import com.novamart.auth.dto.AuthDtos.AddressRequest;
import com.novamart.auth.dto.AuthDtos.AddressResponse;
import com.novamart.auth.repository.AddressRepository;
import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * The shopper address book.
 *
 * <p>Every read and write is scoped by {@code userId} at the query level rather
 * than by loading a row and checking ownership afterwards. That way a request
 * for someone else's address returns "not found" and cannot be distinguished
 * from an address that never existed.
 */
@Service
public class AddressService {

    private final AddressRepository addresses;

    public AddressService(AddressRepository addresses) {
        this.addresses = addresses;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> listFor(UUID userId) {
        return AddressResponse.from(addresses.findByUserIdOrderByDefaultAddressDescCreatedAtAsc(userId));
    }

    @Transactional(readOnly = true)
    public AddressResponse getFor(UUID userId, UUID addressId) {
        return AddressResponse.from(load(userId, addressId));
    }

    @Transactional
    public AddressResponse create(UUID userId, AddressRequest request) {
        Address address = Address.create(userId);
        applyFields(address, request);

        // The first address a shopper saves becomes the default whether they
        // asked for it or not, so checkout always has something to preselect.
        boolean makeDefault = request.isDefault() || addresses.countByUserId(userId) == 0;
        address.markDefault(makeDefault);
        addresses.save(address);

        if (makeDefault) {
            addresses.clearDefaultExcept(userId, address.getId());
        }
        return AddressResponse.from(address);
    }

    @Transactional
    public AddressResponse update(UUID userId, UUID addressId, AddressRequest request) {
        Address address = load(userId, addressId);
        applyFields(address, request);

        if (request.isDefault()) {
            address.markDefault(true);
            addresses.clearDefaultExcept(userId, address.getId());
        }
        return AddressResponse.from(address);
    }

    @Transactional
    public void delete(UUID userId, UUID addressId) {
        Address address = load(userId, addressId);
        boolean wasDefault = address.isDefaultAddress();
        addresses.delete(address);

        if (wasDefault) {
            // Never leave the book without a default, or checkout has nothing to
            // preselect and the shopper has to pick every time.
            addresses.findByUserIdOrderByDefaultAddressDescCreatedAtAsc(userId).stream()
                    .findFirst()
                    .ifPresent(next -> next.markDefault(true));
        }
    }

    private Address load(UUID userId, UUID addressId) {
        return addresses.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.ADDRESS_NOT_FOUND));
    }

    private static void applyFields(Address address, AddressRequest r) {
        address.apply(r.label().trim(), r.recipientName().trim(), r.phone().trim(),
                r.line1().trim(), r.line2(), r.city().trim(), r.state().trim(),
                r.postalCode().trim(), r.country().trim());
    }
}
