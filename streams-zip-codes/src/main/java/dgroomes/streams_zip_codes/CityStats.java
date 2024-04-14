package dgroomes.streams_zip_codes;

/**
 * Statistics at a city level.
 * <p>
 * The city and state fields are purposely not included in this record because they are meant to be specified in the
 * CityState class.
 */
public record CityStats(
        // The number of ZIP areas in the city
        int zipAreas,

        // Total city population
        int totalPop,

        // The average population of ZIP areas in the city
        int avgPop) {
}
