package io.github.voicestt;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.json.JSONObject;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class MicRecognizer {
    private final Model model;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;
    private TargetDataLine line;

    public MicRecognizer(Path modelDir) throws Exception {
        this.model = new Model(modelDir.toString());
    }

    public void start(Consumer<String> onResult, Consumer<String> onError) {
        if (running.getAndSet(true)) return;

        worker = new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();

                Recognizer recognizer = new Recognizer(model, 16000);

                byte[] buffer = new byte[4096];
                String lastResult = "";

                while (running.get()) {
                    int count = line.read(buffer, 0, buffer.length);
                    if (count > 0) {
                        boolean isFinal = recognizer.acceptWaveForm(buffer, count);
                        if (isFinal) {
                            String result = recognizer.getResult();
                            JSONObject json = new JSONObject(result);
                            String text = json.optString("text", "");
                            if (!text.isEmpty() && !text.equals(lastResult)) {
                                lastResult = text;
                            }
                        }
                    }
                }

                // Get final partial result on stop
                String partial = recognizer.getPartialResult();
                JSONObject json = new JSONObject(partial);
                String text = json.optString("text", "");
                if (!text.isEmpty()) {
                    onResult.accept(text);
                }

                recognizer.close();
                line.stop();
                line.close();

            } catch (Exception e) {
                onError.accept(e.getMessage());
                running.set(false);
            }
        }, "MicRecognizer-Worker");
        worker.start();
    }

    public void stop() {
        running.set(false);
        if (worker != null) {
            try {
                worker.join(2000);
            } catch (InterruptedException ignored) {}
            worker = null;
        }
        if (line != null) {
            line.stop();
            line.close();
            line = null;
        }
    }
}
