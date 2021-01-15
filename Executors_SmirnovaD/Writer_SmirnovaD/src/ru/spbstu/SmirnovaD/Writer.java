package ru.spbstu.SmirnovaD;

import javafx.util.Pair;
import ru.spbstu.pipeline.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Integer.min;

public class Writer implements IWriter {
    private final Logger LOGGER;
    private final static TYPE[] supportedInputTypes = {TYPE.BYTE, TYPE.CHAR, TYPE.SHORT};
    private final static int mask = 0x00ff;
    private final static int byteSize = 8;
    private byte[] data;
    private FileOutputStream stream;
    private IMediator mediator;
    private int bufferCapacity;
    private TYPE chosenInputType;
    private volatile int numWaitedChunks;

    public Writer(Logger logger) {
        LOGGER = logger;
        stream = null;
        mediator = null;
        chosenInputType = null;
        data = new byte[0];

    }

    private class Notifier implements INotifier {
        @Override
        public RC notify(int i) {
            synchronized (Writer.this) {
                numWaitedChunks++;
            }
            return RC.CODE_SUCCESS;
        }
    }
    @Override
    public void run() {
        while (data != null) {
            if (numWaitedChunks > 0) {
                synchronized (this) {
                    numWaitedChunks--;
                }
                RC error = execute();
                if (error != RC.CODE_SUCCESS)
                    return;
            }
        }
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
    public RC addNotifier(INotifier iNotifier) {
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setProducer(IProducer process) {
        if (process == null) {
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    RC.CODE_FAILED_PIPELINE_CONSTRUCTION.toString());
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        }
        chosenInputType = findFirstIntersection(supportedInputTypes, process.getOutputTypes());
        if (chosenInputType == null) {
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    RC.CODE_FAILED_PIPELINE_CONSTRUCTION.toString().concat(" can't find suitable exchange type for writer."));
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        }
        mediator = process.getMediator(chosenInputType);
        return RC.CODE_SUCCESS;
    }

    public RC execute() {
        Object mediatorsData = mediator.getData(0);
        if (mediatorsData == null) {
            data = null;
            return RC.CODE_SUCCESS;
        }
        switch (chosenInputType) {
            case BYTE:
                data = (byte[])mediatorsData;
                break;
            case CHAR:
                data = Arrays.toString(((char[]) mediatorsData)).getBytes();
                break;
            case SHORT:
                data = convertToByte((short[])mediatorsData);
                break;
        }
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
    public INotifier getNotifier() {
        return new Notifier();
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

    private TYPE findFirstIntersection(TYPE[] arr1, TYPE[] arr2) {
        for (TYPE type1 : arr1) {
            for (TYPE type2 : arr2) {
                if (type1 == type2)
                    return type1;
            }
        }
        return null;
    }

    private static byte[] convertToByte(short[] array) {
        byte[] newArray = new byte[array.length * 2];
        for (int i = 0; i < array.length; ++i) {
            newArray[2 * i] = (byte)((array[i] & (mask << byteSize)) >> byteSize);
            newArray[2 * i + 1] = (byte)(array[i] & mask);
        }
        return newArray;
    }
}
