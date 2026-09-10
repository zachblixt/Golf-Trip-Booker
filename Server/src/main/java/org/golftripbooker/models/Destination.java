package org.golftripbooker.models;

import java.math.BigDecimal;
import java.util.Objects;

public class Destination {

    private int destinationId;
    private String name;
    private String region;
    private String description;

    /**
     * BigDecimal rather than double, matching the column's decimal(9,6). Coordinates
     * round-trip exactly this way, which matters because Destination.equals compares
     * them and the repository tests assert on whole objects.
     *
     * Nullable: a destination without coordinates is simply absent from the map.
     */
    private BigDecimal latitude;
    private BigDecimal longitude;

    public Destination() {
    }

    public Destination(int destinationId, String name, String region, String description,
                       BigDecimal latitude, BigDecimal longitude) {
        this.destinationId = destinationId;
        this.name = name;
        this.region = region;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /** True only when both halves are present -- half a coordinate cannot be plotted. */
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    public int getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(int destinationId) {
        this.destinationId = destinationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Destination that = (Destination) o;
        return destinationId == that.destinationId
                && Objects.equals(name, that.name)
                && Objects.equals(region, that.region)
                && Objects.equals(description, that.description)
                && Objects.equals(latitude, that.latitude)
                && Objects.equals(longitude, that.longitude);
    }

    @Override
    public int hashCode() {
        return Objects.hash(destinationId, name, region, description, latitude, longitude);
    }
}
