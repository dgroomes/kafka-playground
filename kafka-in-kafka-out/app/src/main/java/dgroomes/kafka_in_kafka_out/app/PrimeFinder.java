package dgroomes.kafka_in_kafka_out.app;

public class PrimeFinder {

    // Check if a number is prime using basic division
    public static boolean isPrime(int num) {
        if (num < 2) return false;
        if (num == 2) return true;
        if (num % 2 == 0) return false;

        // Only need to check up to square root
        for (int i = 3; i <= Math.sqrt(num); i += 2) {
            if (num % i == 0) return false;
        }
        return true;
    }

    /**
     * A naive implementation for finding prime numbers. This method computes the Nth prime number.
     * <p>
     * This is a contrived example of a domain operation that is CPU-intensive.
     */
    public static int findNthPrime(int nth) {
        if (nth < 1) throw new IllegalArgumentException("N must be positive");

        int num = 0;
        int i = 0;
        while (true) {
            if (isPrime(num)) {
                i++;
                if (i == nth) break;
            }
            num++;
        }

        return num;
    }
}
