package org.golftripbooker.dtos;

import org.golftripbooker.models.Destination;

import java.math.BigDecimal;
import java.util.List;

public class DestinationResponse {

    private final int destinationId;
    private final String name;
    private final String region;
    private final String description;
    private final BigDecimal latitude;
    private final BigDecimal longitude;

    public static DestinationResponse from(Destination destination) {
        if (destination == null) {
            return null;
        }
        return new DestinationResponse(
                destination.getDestinationId(),
                destination.getName(),
                destination.getRegion(),
                destination.getDescription(),
                destination.getLatitude(),
                destination.getLongitude());
    }

    public static List<DestinationResponse> fromAll(List<Destination> destinations) {
        return destinations.stream().map(DestinationResponse::from).toList();
    }

    public DestinationResponse(int destinationId, String name, String region, String description,
                               BigDecimal latitude, BigDecimal longitude) {
        this.destinationId = destinationId;
        this.name = name;
        this.region = region;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public int getDestinationId() {
        return destinationId;
    }

    public String getName() {
        return name;
    }

    public String getRegion() {
        return region;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }
}
