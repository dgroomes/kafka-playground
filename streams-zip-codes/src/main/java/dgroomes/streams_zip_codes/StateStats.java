package dgroomes.streams_zip_codes;

/**
 * Statistics at a state level.
 */
public record StateStats(
        // The number of ZIP areas in the state
        int zipAreas,

        // Total state population
        int totalPop,

        // The average population of ZIP areas in the state
        int avgPop) {
}
