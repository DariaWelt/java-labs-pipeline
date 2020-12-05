package ru.spbstu.SmirnovaD;

import javafx.util.Pair;
import ru.spbstu.pipeline.RC;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

enum Mode {
    MODE_READER,
    MODE_WRITER,
    MODE_ENCODER
}
public class AppParser {
    final static String separator = "=";

    /** brief
     * Синтаксический парсинг конфигурационного файла. Построчно разделяем строку на два элемента согласно разделителю "=".
     * Если получили не ровно две подстроки, возвращаем null.
     * @param fileName путь к файлу, который мы парсим
     * @param LOGGER логер, куда записываются ошибки
     * @return map, где ключ соответствует левому операнду грамматики, а значение - правому
     */
    public static Map<String,String> syntaxParse(String fileName, Logger LOGGER) {
        Map<String,String> map = new TreeMap<>();
        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            stream.forEach(obj -> {
                String[] arr = obj.split(separator);
                if (arr.length == 2) {
                    map.put(arr[0], arr[1]);
                }
            });
            return map;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    RC.CODE_INVALID_ARGUMENT.toString());
            return null;
        }
    }

    /** brief Парсинг конфигурационного файла из одного значения Integer с проверкой значения на валидность
     *
     * @param configPath путь к конфигурационному файлу
     * @param LOGGER логгер, куда будут записываться данные времени выполнения о ходе программы
     * @param mode значение характеризует, к какому классу относится параметр, который мы проверяем
     * @return пару: значение Integer, полученное парсингом или null и сообщение об ошибке
     */
    public static Pair<Integer, RC> semanticParse(String configPath, Logger LOGGER, Mode mode) {
        int value;
        Map<String,String> map = syntaxParse(configPath, LOGGER);
        if (map == null || map.size() != 1) {
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    RC.CODE_CONFIG_GRAMMAR_ERROR.toString());
            return new Pair<> (0, RC.CODE_CONFIG_GRAMMAR_ERROR);
        }
        try {
            if (mode == Mode.MODE_ENCODER)
                value = Integer.parseInt(map.get(AppConfigParams.SHIFT_AMOUNT.toString()));
            else if (mode == Mode.MODE_READER || mode == Mode.MODE_WRITER) {
                value = Integer.parseInt(map.get(AppConfigParams.BUFFER_CAPACITY.toString()));
                if (value <= 0)
                    throw new NumberFormatException();
            }
            else
                throw new NumberFormatException();
        } catch (NullPointerException| NumberFormatException e) {
            LOGGER.log(Level.WARNING, LogType.FAULT_IN_METHOD.toString(),
                    RC.CODE_CONFIG_SEMANTIC_ERROR.toString());
             return new Pair<> (0, RC.CODE_CONFIG_SEMANTIC_ERROR);
        }
        return  new Pair<>(value, RC.CODE_SUCCESS);
    }

}
