package com.upstart.backend.util;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for distance and location calculations
 * Provides methods for calculating distances, estimating delivery times, and handling coordinates
 */
@Component
public class DistanceCalculationUtils {

    private static final Logger logger = LoggerFactory.getLogger(DistanceCalculationUtils.class);

    // Earth's radius in kilometers
    private static final double EARTH_RADIUS_KM = 6371.0;
    
    // Average delivery speeds (km/h)
    private static final double URBAN_SPEED_KMH = 25.0;
    private static final double SUBURBAN_SPEED_KMH = 40.0;
    private static final double HIGHWAY_SPEED_KMH = 60.0;
    
    // Base delivery time in minutes
    private static final int BASE_DELIVERY_TIME_MINUTES = 15;
    
    // Coordinate patterns for parsing
    private static final Pattern COORDINATE_PATTERN = Pattern.compile(
        "(-?\\d+\\.\\d+),\\s*(-?\\d+\\.\\d+)");

    /**
     * Calculate straight-line distance between two coordinates using Haversine formula
     */
    public BigDecimal calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert latitude and longitude from degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Calculate differences
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;

        // Haversine formula
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_KM * c;

        return BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate distance between two coordinate strings
     */
    public BigDecimal calculateDistance(String coordinates1, String coordinates2) {
        try {
            double[] coord1 = parseCoordinates(coordinates1);
            double[] coord2 = parseCoordinates(coordinates2);
            
            return calculateHaversineDistance(coord1[0], coord1[1], coord2[0], coord2[1]);
        } catch (Exception e) {
            logger.warn("Failed to calculate distance between coordinates: {} and {}", 
                       coordinates1, coordinates2, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Estimate driving distance (approximately 1.3x straight-line distance)
     */
    public BigDecimal estimateDrivingDistance(String coordinates1, String coordinates2) {
        BigDecimal straightLineDistance = calculateDistance(coordinates1, coordinates2);
        return straightLineDistance.multiply(BigDecimal.valueOf(1.3))
                                 .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Estimate delivery time based on distance and area type
     */
    public int estimateDeliveryTimeMinutes(BigDecimal distanceKm, String areaType) {
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) <= 0) {
            return BASE_DELIVERY_TIME_MINUTES;
        }

        double distance = distanceKm.doubleValue();
        double speed = getSpeedForAreaType(areaType);
        
        // Calculate travel time in hours, then convert to minutes
        double travelTimeHours = distance / speed;
        int travelTimeMinutes = (int) Math.ceil(travelTimeHours * 60);
        
        // Add base delivery time for pickup/dropoff
        return travelTimeMinutes + BASE_DELIVERY_TIME_MINUTES;
    }

    /**
     * Estimate delivery time using coordinates
     */
    public int estimateDeliveryTime(String pickupCoordinates, String deliveryCoordinates, String areaType) {
        BigDecimal distance = estimateDrivingDistance(pickupCoordinates, deliveryCoordinates);
        return estimateDeliveryTimeMinutes(distance, areaType);
    }

    /**
     * Check if coordinates are within a specified radius
     */
    public boolean isWithinRadius(String centerCoordinates, String targetCoordinates, double radiusKm) {
        BigDecimal distance = calculateDistance(centerCoordinates, targetCoordinates);
        return distance.doubleValue() <= radiusKm;
    }

    /**
     * Calculate delivery cost based on distance
     */
    public BigDecimal calculateDistanceBasedCost(BigDecimal distanceKm, BigDecimal baseRate, BigDecimal perKmRate) {
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) <= 0) {
            return baseRate != null ? baseRate : BigDecimal.ZERO;
        }

        BigDecimal baseCost = baseRate != null ? baseRate : BigDecimal.valueOf(5.00);
        BigDecimal distanceCost = distanceKm.multiply(perKmRate != null ? perKmRate : BigDecimal.valueOf(0.50));
        
        return baseCost.add(distanceCost).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Get the center point (centroid) of multiple coordinates
     */
    public String calculateCentroid(String... coordinatesList) {
        if (coordinatesList == null || coordinatesList.length == 0) {
            return null;
        }

        double totalLat = 0.0;
        double totalLon = 0.0;
        int validCount = 0;

        for (String coordinates : coordinatesList) {
            try {
                double[] coord = parseCoordinates(coordinates);
                totalLat += coord[0];
                totalLon += coord[1];
                validCount++;
            } catch (Exception e) {
                logger.warn("Invalid coordinates in centroid calculation: {}", coordinates);
            }
        }

        if (validCount == 0) {
            return null;
        }

        double avgLat = totalLat / validCount;
        double avgLon = totalLon / validCount;

        return String.format("%.6f,%.6f", avgLat, avgLon);
    }

    /**
     * Find the closest coordinate from a list
     */
    public String findClosestCoordinate(String referenceCoordinates, String... candidateCoordinates) {
        if (candidateCoordinates == null || candidateCoordinates.length == 0) {
            return null;
        }

        String closest = null;
        BigDecimal minDistance = null;

        for (String candidate : candidateCoordinates) {
            try {
                BigDecimal distance = calculateDistance(referenceCoordinates, candidate);
                if (minDistance == null || distance.compareTo(minDistance) < 0) {
                    minDistance = distance;
                    closest = candidate;
                }
            } catch (Exception e) {
                logger.warn("Error calculating distance to candidate: {}", candidate);
            }
        }

        return closest;
    }

    /**
     * Validate coordinate format
     */
    public boolean isValidCoordinateFormat(String coordinates) {
        if (coordinates == null || coordinates.trim().isEmpty()) {
            return false;
        }

        try {
            parseCoordinates(coordinates);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Convert address to approximate coordinates (simplified implementation)
     * In a real application, this would integrate with a geocoding service
     */
    public String addressToCoordinates(String address) {
        if (address == null || address.trim().isEmpty()) {
            return null;
        }

        // Simplified implementation - returns mock coordinates
        // In real implementation, integrate with Google Maps API, OpenStreetMap, etc.
        logger.info("Mock geocoding for address: {}", address);
        
        // Return mock coordinates for demonstration
        // This should be replaced with actual geocoding service integration
        return "40.7128,-74.0060"; // New York City coordinates as default
    }

    /**
     * Convert coordinates to approximate address (simplified implementation)
     * In a real application, this would integrate with a reverse geocoding service
     */
    public String coordinatesToAddress(String coordinates) {
        if (!isValidCoordinateFormat(coordinates)) {
            return null;
        }

        // Simplified implementation - returns mock address
        // In real implementation, integrate with reverse geocoding service
        logger.info("Mock reverse geocoding for coordinates: {}", coordinates);
        
        return "Mock Address, City, State"; // Mock address for demonstration
    }

    /**
     * Calculate bounding box for a given center point and radius
     */
    public BoundingBox calculateBoundingBox(String centerCoordinates, double radiusKm) {
        try {
            double[] center = parseCoordinates(centerCoordinates);
            double lat = center[0];
            double lon = center[1];

            // Calculate latitude and longitude deltas
            double latDelta = radiusKm / 111.0; // Approximately 111 km per degree of latitude
            double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

            double minLat = lat - latDelta;
            double maxLat = lat + latDelta;
            double minLon = lon - lonDelta;
            double maxLon = lon + lonDelta;

            return new BoundingBox(minLat, maxLat, minLon, maxLon);
        } catch (Exception e) {
            logger.error("Error calculating bounding box for coordinates: {}", centerCoordinates, e);
            return null;
        }
    }

    /**
     * Check if coordinates are within a bounding box
     */
    public boolean isWithinBoundingBox(String coordinates, BoundingBox boundingBox) {
        try {
            double[] coord = parseCoordinates(coordinates);
            double lat = coord[0];
            double lon = coord[1];

            return lat >= boundingBox.getMinLat() && lat <= boundingBox.getMaxLat() &&
                   lon >= boundingBox.getMinLon() && lon <= boundingBox.getMaxLon();
        } catch (Exception e) {
            logger.warn("Error checking bounding box for coordinates: {}", coordinates);
            return false;
        }
    }

    /**
     * Parse coordinate string into latitude and longitude
     */
    private double[] parseCoordinates(String coordinates) {
        if (coordinates == null || coordinates.trim().isEmpty()) {
            throw new IllegalArgumentException("Coordinates cannot be null or empty");
        }

        Matcher matcher = COORDINATE_PATTERN.matcher(coordinates.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid coordinate format: " + coordinates);
        }

        double lat = Double.parseDouble(matcher.group(1));
        double lon = Double.parseDouble(matcher.group(2));

        // Validate coordinate ranges
        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90: " + lat);
        }
        if (lon < -180 || lon > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180: " + lon);
        }

        return new double[]{lat, lon};
    }

    /**
     * Get speed based on area type
     */
    private double getSpeedForAreaType(String areaType) {
        if (areaType == null) {
            return SUBURBAN_SPEED_KMH;
        }

        switch (areaType.toLowerCase()) {
            case "urban":
            case "city":
            case "downtown":
                return URBAN_SPEED_KMH;
            case "highway":
            case "interstate":
                return HIGHWAY_SPEED_KMH;
            case "suburban":
            case "residential":
            default:
                return SUBURBAN_SPEED_KMH;
        }
    }

    /**
     * Bounding box class for geographic calculations
     */
    public static class BoundingBox {
        private double minLat;
        private double maxLat;
        private double minLon;
        private double maxLon;

        public BoundingBox(double minLat, double maxLat, double minLon, double maxLon) {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLon = minLon;
            this.maxLon = maxLon;
        }

        // Getters
        public double getMinLat() { return minLat; }
        public double getMaxLat() { return maxLat; }
        public double getMinLon() { return minLon; }
        public double getMaxLon() { return maxLon; }

        @Override
        public String toString() {
            return String.format("BoundingBox{minLat=%.6f, maxLat=%.6f, minLon=%.6f, maxLon=%.6f}",
                               minLat, maxLat, minLon, maxLon);
        }
    }

    /**
     * Distance calculation result with additional metadata
     */
    public static class DistanceResult {
        private BigDecimal straightLineDistance;
        private BigDecimal estimatedDrivingDistance;
        private int estimatedTimeMinutes;
        private BigDecimal estimatedCost;

        public DistanceResult(BigDecimal straightLineDistance, BigDecimal estimatedDrivingDistance,
                            int estimatedTimeMinutes, BigDecimal estimatedCost) {
            this.straightLineDistance = straightLineDistance;
            this.estimatedDrivingDistance = estimatedDrivingDistance;
            this.estimatedTimeMinutes = estimatedTimeMinutes;
            this.estimatedCost = estimatedCost;
        }

        // Getters
        public BigDecimal getStraightLineDistance() { return straightLineDistance; }
        public BigDecimal getEstimatedDrivingDistance() { return estimatedDrivingDistance; }
        public int getEstimatedTimeMinutes() { return estimatedTimeMinutes; }
        public BigDecimal getEstimatedCost() { return estimatedCost; }

        @Override
        public String toString() {
            return String.format("DistanceResult{straightLine=%.2f km, driving=%.2f km, time=%d min, cost=$%.2f}",
                               straightLineDistance.doubleValue(), estimatedDrivingDistance.doubleValue(),
                               estimatedTimeMinutes, estimatedCost.doubleValue());
        }
    }

    /**
     * Calculate comprehensive distance result
     */
    public DistanceResult calculateComprehensiveDistance(String pickupCoordinates, String deliveryCoordinates,
                                                       String areaType, BigDecimal baseRate, BigDecimal perKmRate) {
        BigDecimal straightLine = calculateDistance(pickupCoordinates, deliveryCoordinates);
        BigDecimal driving = estimateDrivingDistance(pickupCoordinates, deliveryCoordinates);
        int timeMinutes = estimateDeliveryTimeMinutes(driving, areaType);
        BigDecimal cost = calculateDistanceBasedCost(driving, baseRate, perKmRate);

        return new DistanceResult(straightLine, driving, timeMinutes, cost);
    }
}