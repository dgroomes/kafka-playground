package dgroomes.interactive;

import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class keeps references to the metric-subsystem defined by the Kafka client library. The convenience of this
 * class is by the "metrics map" that it exposes. This can be accessed to find the current metric value (limited to
 * some specific metrics, but this is extensible).
 */
public class Metrics implements MetricsReporter {

    public static final String METRIC_LAG = "records-lag";
    public static final Map<String, Set<KafkaMetric>> metricsMap = new ConcurrentHashMap<>();

    private static final Logger log = LoggerFactory.getLogger(Metrics.class);

    /**
     * For the sake of reducing log noise, let's just focus on some specific metrics. The Kafka client library defines
     * dozens of logs so if we tracked all of them we would create noise in the logs.
     */
    private final Set<String> interestingMetrics = Set.of(METRIC_LAG);


    /**
     * There must be a no-args constructor on this class. While we can just omit any constructor and get the default
     * no-args constructor for free, I wanted to highlight the fact that this class will be new-ed up by the Kafka
     * client library because it is referenced in the "metric.reporters" client configuration. The Kafka client library
     * is only smart enough to new up class referenced in a configuration property via a no-args constructor.
     */
    public Metrics() {
        log.debug("The '{}' class is being instantiated (probably by the Kafka client library)", Metrics.class.getSimpleName());
    }

    @Override
    public void init(List<KafkaMetric> metrics) {
        for (KafkaMetric metric : metrics) {
            addMetric("init", metric);
        }
    }

    /**
     * This interface method is a bit misleadingly named. It says "metricChange" but this does not mean when the metric
     * *value* has changed. So you can't rely on this method as a callback that will conveniently alert you when a metric
     * value has changed.
     */
    @Override
    public void metricChange(KafkaMetric metric) {
        addMetric("metricChange", metric);
    }

    /**
     * Add the metric to the "metrics map" if it doesn't already exist. The metric will be added using a key value
     * equal to the metric's "metricName()" value.
     *
     * @param method the MetricsReport lifecycle method that invoked this
     * @param metric the metric to add
     */
    private void addMetric(String method, KafkaMetric metric) {
        String name = metric.metricName().name();
        if (!interestingMetrics.contains(name)) {
            return; // not an interesting metric. Skip it!
        }

        log.trace("[{}] {}", method, metric.metricName());
        Metrics.metricsMap.compute(name, (key, existing) -> {
            Set<KafkaMetric> set;
            set = Objects.requireNonNullElseGet(existing, HashSet::new);
            set.add(metric);
            return set;
        });
    }

    @Override
    public void metricRemoval(KafkaMetric metric) {

    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> configs) {

    }
}
