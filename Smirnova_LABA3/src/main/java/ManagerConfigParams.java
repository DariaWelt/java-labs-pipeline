package main.java;

public enum ManagerConfigParams {
    INPUT_STREAM, OUTPUT_STREAM, WRITER_CONF, READER_CONF,
    PROCESSES_AMOUNT, CONFIG_PATH, PROCESS_NAME;

    static final private String[] paramValues = {
            "inputStream",
            "outputStream",
            "writer",
            "reader",
            "processesAmount",
            "configuration",
            "process"};

    @Override
    public String toString() {
        return paramValues[this.ordinal()];
    }

}
