package dgroomes.kafka_in_kafka_out.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class LowestWord {
    /**
     * Sort the words in the string (separated by space) in alphanumeric order and then get the lowest word.
     * <p>
     * This is a contrived example of a domain operation that is CPU-intensive.
     */
    public static String lowest(String text, int sortFactor) {
        var words = new ArrayList<>(Arrays.asList(text.split(" ")));
        for (int i = 0; i < sortFactor; i++) {
            Collections.shuffle(words);
            Collections.sort(words);
        }

        if (words.isEmpty()) {
            return "";
        } else {
            return words.getFirst();
        }
    }
}
