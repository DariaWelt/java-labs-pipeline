package ru.spbstu.SmirnovaD;

enum AppConfigParams {
    SHIFT_AMOUNT, BUFFER_CAPACITY;

    static final private String[] paramValues = {
            "shiftAmount",
            "bufferCapacity"};

    @Override
    public String toString() {
        return paramValues[this.ordinal()];
    }
}
