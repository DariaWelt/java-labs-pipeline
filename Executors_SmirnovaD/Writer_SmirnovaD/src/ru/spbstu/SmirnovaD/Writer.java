package ru.spbstu.SmirnovaD;

import javafx.util.Pair;
import ru.spbstu.pipeline.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Integer.min;

public class Writer implements IWriter {
    private final Logger LOGGER;
    private FileOutputStream stream;
    private IExecutable producer;
    private int bufferCapacity;

    public Writer(Logger logger) {
        LOGGER = logger;
        stream = null;
        producer = null;
    }

    @Override
    public RC setConfig(String config) {
        if (config == null) {
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(), "Argument of method is null. ");
            return RC.CODE_INVALID_ARGUMENT;
        }
        Pair<Integer, RC> result = AppParser.semanticParse(config, LOGGER, Mode.MODE_WRITER);
        if (result.getValue() != RC.CODE_SUCCESS)
            return result.getValue();
        bufferCapacity = result.getKey();
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setConsumer(IExecutable process) {
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setProducer(IExecutable process) {
        if (process == null) {
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    RC.CODE_FAILED_PIPELINE_CONSTRUCTION.toString());
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        }
        producer = process;
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC execute(byte[] data) {
        byte[] buffer;
        try {
            for (int i = 0; i < data.length; i += bufferCapacity){
                buffer = Arrays.copyOfRange(data, i, i + min(bufferCapacity, data.length - i));
                stream.write(buffer);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    RC.CODE_FAILED_TO_WRITE.toString());
            return RC.CODE_FAILED_TO_WRITE;
        }
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setOutputStream(FileOutputStream output) {
        if (output == null) {
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    RC.CODE_FAILED_PIPELINE_CONSTRUCTION.toString());
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        }
        stream = output;
        return RC.CODE_SUCCESS;
    }
}
