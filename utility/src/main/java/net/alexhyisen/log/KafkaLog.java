package net.alexhyisen.log;

import net.alexhyisen.Config;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class KafkaLog implements Log {
    private static final ReentrantLock lock = new ReentrantLock();
    private static KafkaLog instance = null;
    private Producer<String, String> producer;

    private KafkaLog() {
        var config = new Config();
        config.load();
        String addr = config.get("kafkaBootstrapServers");
        if (addr == null) {
            addr = "localhost:9092";
            System.out.println("ERROR: can not get kafkaBootstrapServers from Config.\n" +
                    "use default " + addr + " instead.");
        }
        this.producer = new KafkaProducer<>(Map.of(
                "bootstrap.servers", addr,
                "acks", "all",
                "key.serializer", "org.apache.kafka.common.serialization.StringSerializer",
                "value.serializer", "org.apache.kafka.common.serialization.StringSerializer",
                "buffer.memory", "1048576",
                "client.id", "Eta0"
        ));
    }

    //Of course I can use synchronize(lock) to implements the exactly-once initializing procedure,
    //which could be even faster because of the synchronize optimization such as biased lock.
    //But, first we shall never put optimization of one-shot procedure on a recognizable priority,
    //secondly, no significant difference appears on the benchmark. (Swap the order would flap the result.)
    public static KafkaLog getInstance() {
        if (instance == null) {
            lock.lock();
            try {
                if (instance == null) {
                    instance = new KafkaLog();
                }
            } finally {
                lock.unlock();
            }
        }
        return instance;
    }

    public static void main(String[] args) {
        Log log = new KafkaLog();
        log.log(LogCls.INFO, "ECHO");
        log.log(LogCls.INFO, "HELO");

        log.shutdownGlobally();
    }

    //Reference Counter such as std::shared_ptr with destructor can solve the problem,
    //but as the Object::finalize() has no guarantee to be executed,
    //and AutoClosable must come up with try with resource, which is easy in function but hard in class.
    //I would rather use a global shutdown instead of multiple identical protective boilerplate.

    @Override
    public void log(LogCls type, String message) {
        producer.send(new ProducerRecord<>(type.getPath(), map(type, message)));
    }

    /**
     * <p>{@inheritDoc}</p>
     * Little punishment would occur if user forgets to shutdown, only the lost of possible unsent message.
     */
    @Override
    public void shutdownGlobally() {
        producer.close();
    }
}

