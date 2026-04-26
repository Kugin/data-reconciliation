package io.github.kugin.reconciliation.before;

import cn.hutool.core.text.CharSequenceUtil;
import io.github.kugin.reconciliation.domain.CheckEntry;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;

import java.util.List;
import java.util.stream.Collectors;

public class RedisResourceLoader<T> implements ResourceLoader {

    private final String redisKeyPattern;
    private final String identityField;
    private final List<String> checkFields;
    private final RedisEntityParser<T> redisEntityParser;
    private final RedissonClient redissonClient;

    public RedisResourceLoader(String redisKeyPattern, String identityField,
                               List<String> checkFields, RedisEntityParser<T> redisEntityParser,
                               RedissonClient redissonClient) {
        this.redisKeyPattern = redisKeyPattern;
        this.identityField = identityField;
        this.checkFields = checkFields;
        this.redisEntityParser = redisEntityParser;
        this.redissonClient = redissonClient;
    }

    public String getRedisKey(String date) {
        return String.format(redisKeyPattern, date);
    }

    @Override
    public List<CheckEntry> load(String date) {
        RList<String> list = redissonClient.getList(getRedisKey(date));
        List<T> entities = list.readAll().stream()
                .map(redisEntityParser::parse)
                .collect(Collectors.toList());
        if (CharSequenceUtil.isEmpty(identityField) && (checkFields == null || checkFields.isEmpty())) {
            return CheckEntry.wrap(entities);
        }
        return CheckEntry.wrap(entities, identityField, checkFields);
    }

    @FunctionalInterface
    public interface RedisEntityParser<T> {
        T parse(String line);
    }
}
