package ru.spbstu.SmirnovaD;

import javafx.util.Pair;
import ru.spbstu.SmirnovaD.*;
import ru.spbstu.pipeline.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Reader implements IReader {
    private final Logger LOGGER;
    private FileInputStream stream;
    private IExecutable consumer;
    private int bufferCapacity;

    public Reader(Logger logger) {
        LOGGER = logger;
        stream = null;
        consumer = null;
    }

    @Override
    public RC setConfig(String config) {
        if (config == null) {
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(), "Argument of method is null. ");
            return RC.CODE_INVALID_ARGUMENT;
        }
        Pair<Integer, RC> result = AppParser.semanticParse(config, LOGGER, Mode.MODE_READER);
        if (result.getValue() != RC.CODE_SUCCESS)
            return result.getValue();
        bufferCapacity = result.getKey();
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setConsumer(IExecutable process) {
        if (process == null) {
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    RC.CODE_FAILED_PIPELINE_CONSTRUCTION.toString());
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        }
        consumer = process;
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setProducer(IExecutable process) {
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC execute(byte[] data) { // data == null
        byte[] buffer = new byte[bufferCapacity];
        try {
            int readed = stream.read(buffer);
            while (readed >= 0) {
                byte[] readedData = Arrays.copyOfRange(buffer, 0, readed);
                RC err = consumer.execute(readedData);
                if (err != RC.CODE_SUCCESS)
                    return err;
                readed = stream.read(buffer);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    RC.CODE_FAILED_TO_READ.toString());
            return RC.CODE_FAILED_TO_READ;
        }
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setInputStream(FileInputStream input) {
        if (input == null) {
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    RC.CODE_FAILED_PIPELINE_CONSTRUCTION.toString());
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        }
        stream = input;
        return RC.CODE_SUCCESS;
    }
}
