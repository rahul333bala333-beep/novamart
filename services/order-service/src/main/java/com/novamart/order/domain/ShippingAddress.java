package com.novamart.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * The delivery address copied onto the order at purchase time.
 *
 * <p>An {@code @Embeddable} rather than a related entity: these columns have no
 * life of their own, are never queried independently, and must never change once
 * written.
 */
@Embeddable
public class ShippingAddress {

    @Column(name = "ship_label", length = 40)
    private String label;

    @Column(name = "ship_recipient", nullable = false, length = 120)
    private String recipientName;

    @Column(name = "ship_phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "ship_line1", nullable = false, length = 200)
    private String line1;

    @Column(name = "ship_line2", length = 200)
    private String line2;

    @Column(name = "ship_city", nullable = false, length = 80)
    private String city;

    @Column(name = "ship_state", nullable = false, length = 80)
    private String state;

    @Column(name = "ship_postal_code", nullable = false, length = 16)
    private String postalCode;

    @Column(name = "ship_country", nullable = false, length = 80)
    private String country;

    protected ShippingAddress() {
    }

    public ShippingAddress(String label, String recipientName, String phone, String line1, String line2,
                           String city, String state, String postalCode, String country) {
        this.label = label;
        this.recipientName = recipientName;
        this.phone = phone;
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }

    public String getLabel() {
        return label;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getPhone() {
        return phone;
    }

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }
}
