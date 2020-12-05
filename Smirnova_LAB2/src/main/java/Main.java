package main.java;

import ru.spbstu.pipeline.RC;

import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main (String[] ars) {

        if (ars == null || ars.length <= 0) {
            System.out.println(RC.CODE_INVALID_ARGUMENT.toString());
            return;
        }
        PipelineManager pipeline = new PipelineManager(LOGGER, ars[0]);
        RC error = pipeline.getError();
        if (error != RC.CODE_SUCCESS) {
            return;
        }
        pipeline.run();
        if (pipeline.getError() != RC.CODE_SUCCESS) {
            return;
        }
    }
}
