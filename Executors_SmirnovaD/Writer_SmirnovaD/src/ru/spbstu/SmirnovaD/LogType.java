package ru.spbstu.SmirnovaD;

public enum LogType {
    START_METHOD, FAULT_IN_METHOD, SUCCESS_FINISH_METHOD;

    static final private String[] descryption = {"The method started with paramerers {0}",
            "The method fault because of {0}", "The method successfully finished"};

    @Override
    public String toString() {
        return descryption[this.ordinal()];
    }
}
