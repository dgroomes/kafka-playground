package dgroomes.streams_zip_codes;

/**
 * City and state pair. This is used as a unique identifier for cities. If there was such a thing as a unique city identifier
 * number (is there?) then that would be convenient and fewer bytes. But this is fine.
 */
public record City(
        String city,
        String state) {
}
