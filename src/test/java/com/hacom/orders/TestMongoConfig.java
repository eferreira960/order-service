package com.hacom.orders;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

/**
 * Test configuration for Mongo slice tests.
 * Registers JSR-310 converters for OffsetDateTime (flapdoodle doesn't register them automatically).
 */
@Configuration
public class TestMongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                new OffsetDateTimeToDateConverter(),
                new DateToOffsetDateTimeConverter()
        ));
    }

    static class OffsetDateTimeToDateConverter implements Converter<OffsetDateTime, Date> {
        @Override
        public Date convert(OffsetDateTime source) {
            return Date.from(source.toInstant());
        }
    }

    static class DateToOffsetDateTimeConverter implements Converter<Date, OffsetDateTime> {
        @Override
        public OffsetDateTime convert(Date source) {
            return OffsetDateTime.ofInstant(source.toInstant(), ZoneOffset.UTC);
        }
    }
}