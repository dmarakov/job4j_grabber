package ru.job4j.grabber.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class HabrCareerDateTimeParserTest {

    @Test
    public void whenDatesTheSame() {
        HabrCareerDateTimeParser tracker = new HabrCareerDateTimeParser();
        String inputDate = "2023-07-02T18:34:40+03:00";
        LocalDateTime expected = LocalDateTime.of(2023, 7, 2, 18, 34, 40);
        assertThat(expected).isEqualTo(tracker.parse(inputDate));
    }
}