package dgroomes.streams_zip_codes;

/**
 * Represents the "ZIP area" JSON message model on the input Kafka topic.
 */
public record ZipArea(
        String _id,
        String city,
        String state,
        int pop
) {
}
