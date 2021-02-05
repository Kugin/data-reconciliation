package io.github.kugin.reconciliation.before;

import io.github.kugin.reconciliation.domain.CheckEntry;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * todo 功能未完善
 *
 * @author Kugin
 */
public class FileResourceLoader<T> implements ResourceLoader {

    private String filePathFormat;

    private String identityField;

    private List<String> checkFields;

    private FileEntityParser<T> fileEntityParser;

    public FileResourceLoader(String filePathFormat, String identityField, List<String> checkFields, FileEntityParser<T> fileEntityParser) {
        this.filePathFormat = filePathFormat;
        this.identityField = identityField;
        this.checkFields = checkFields;
        this.fileEntityParser = fileEntityParser;
    }

    public String getFilePath(String date) {
        return String.format(filePathFormat, date);
    }

    @Override
    public List<CheckEntry> load(String date) {
        List<T> list = readFile(getFilePath(date));
        return CheckEntry.wrap(list, identityField, checkFields);
    }

    @SneakyThrows
    public List<T> readFile(String filePath) {
        return Files.readAllLines(Paths.get(filePath)).stream().map(v -> fileEntityParser.parse(v)).collect(Collectors.toList());
    }

    @FunctionalInterface
    public interface FileEntityParser<T> {
        T parse(String strs);
    }
}
