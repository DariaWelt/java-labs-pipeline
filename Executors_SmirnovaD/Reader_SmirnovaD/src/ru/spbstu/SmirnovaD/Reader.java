package ru.spbstu.SmirnovaD;

import javafx.util.Pair;
import ru.spbstu.SmirnovaD.*;
import ru.spbstu.pipeline.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Reader implements IReader {
    private final Logger LOGGER;
    private final static TYPE[] supportedOutputTypes = {TYPE.BYTE, TYPE.SHORT, TYPE.CHAR};
    private final static int mask = 0x00ff;
    private final static int byteSize = 8;

    private FileInputStream stream;
    private IConsumer consumer;
    private int bufferCapacity;
    private byte[] processedData;


    public Reader(Logger logger) {
        LOGGER = logger;
        stream = null;
        consumer = null;
        processedData = null;
    }

    private class Mediator implements IMediator {
        private final TYPE returnedType;
        Mediator(TYPE type) {
            returnedType = type;
        }
        @Override
        public Object getData() {
            if (processedData == null)
                return null;
            switch (returnedType) {
                case BYTE: {
                    byte[] newArray = new byte[processedData.length];
                    System.arraycopy(processedData, 0, newArray, 0, newArray.length);
                    return newArray;
                }
                case SHORT: {
                    short[] newArray = new short[processedData.length / 2];
                    for (int i = 0; i < processedData.length / 2; ++i ){
                        byte hiWord = processedData[2 * i];
                        byte loWord = processedData[2 * i + 1];
                        newArray[i] = (short)((hiWord << byteSize) | loWord & mask);
                    }
                    return newArray;
                }
                case CHAR:{
                    String text = new String(processedData, StandardCharsets.UTF_8);
                    return text.toCharArray();
                }
                default:
                    return null;
            }
        }
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
    public RC setConsumer(IConsumer process) {
        if (process == null) {
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    RC.CODE_FAILED_PIPELINE_CONSTRUCTION.toString());
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        }
        consumer = process;
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setProducer(IProducer iProducer) {
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC execute() {
        byte[] buffer = new byte[bufferCapacity];
        try {
            int readed = stream.read(buffer);
            while (readed >= 0) {
                if (readed % 2 != 0)
                    readed += 1;
                processedData = Arrays.copyOfRange(buffer, 0, readed);
                RC err = consumer.execute();
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

    @Override
    public TYPE[] getOutputTypes() {
        return supportedOutputTypes;
    }

    @Override
    public IMediator getMediator(TYPE type) {
        return new Mediator(type);
    }
}
