package comp3011.assignment1.service;

import java.util.concurrent.atomic.DoubleAdder;
import org.springframework.stereotype.Service;

@Service
public class SubtitleUsageService {
    private final DoubleAdder cumulativeSeconds = new DoubleAdder();

    public void addSeconds(double seconds) {
        if (Double.isFinite(seconds) && seconds > 0) {
            cumulativeSeconds.add(seconds);
        }
    }

    public double totalSeconds() {
        return cumulativeSeconds.sum();
    }
}
