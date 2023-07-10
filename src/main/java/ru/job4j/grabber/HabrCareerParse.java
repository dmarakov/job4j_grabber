package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HabrCareerParse implements Parse {

    private final DateTimeParser dateTimeParser;

    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final int PAGE_NUMBER = 5;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    public static void main(String[] args) throws IOException {
        String link = String.format("%s/vacancies/java_developer?page=", SOURCE_LINK);
        HabrCareerParse hcp = new HabrCareerParse(new HabrCareerDateTimeParser());
        System.out.println(hcp.list(String.format("%s", link)));
    }

    private static String retrieveDescription(String link) throws IOException {
        Connection connection = Jsoup.connect(link);
        Document document = connection.get();
        Element element = document.select(".vacancy-description__text").first();
        return element.text();
    }

    @Override
    public List<Post> list(String link) throws IOException {
        HabrCareerDateTimeParser dtp = new HabrCareerDateTimeParser();
        ArrayList<Post> list = new ArrayList<>();
        for (int i = 1; i <= PAGE_NUMBER; i++) {
            Connection connection = Jsoup.connect(String.format("%s%s", link, i));
            Document document = connection.get();
            Elements rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> {
                Element titleElement = row.select(".vacancy-card__title").first();
                Element linkElement = titleElement.child(0);
                Element timeElement = row.select(".vacancy-card__date").first().child(0);
                String vacancyName = titleElement.text();
                String vacancyLink = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                String date = timeElement.attr("datetime");
                try {
                    list.add(createPost(vacancyName, vacancyLink, retrieveDescription(vacancyLink), dtp.parse(date)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return list;
    }

    public static Post createPost(String vacancyName, String link, String description, LocalDateTime date) {
        return new Post(vacancyName, link, description, date);
    }
}