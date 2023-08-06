package ru.job4j.grabber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.job4j.quartz.AlertRabbit;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class PsqlStore implements Store {

    private Connection cnn;
    private static final Logger LOG = LoggerFactory.getLogger(PsqlStore.class.getName());

    public PsqlStore(Properties cfg) throws SQLException {
        try {
            Class.forName(cfg.getProperty("jdbc.driver"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        String url = loadProperties().getProperty("jdbc.url");
        String login = loadProperties().getProperty("jdbc.username");
        String password = loadProperties().getProperty("jdbc.password");
        cnn = DriverManager.getConnection(url, login, password);
    }

    public static Properties loadProperties() {
        Properties config = new Properties();
        try (InputStream in = AlertRabbit.class.getClassLoader().getResourceAsStream("liquibase.properties")) {
            config.load(in);
        } catch (Exception e) {
            LOG.error("Exception", e);
        }
        return config;
    }

    @Override
    public void save(Post post) {
        try (PreparedStatement ps = cnn.prepareStatement("INSERT INTO post(name,text,link,created) VALUES(?,?,?,?)" +
                " on conflict (link) do nothing", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, post.getTitle());
            ps.setString(2, post.getDescription());
            ps.setString(3, post.getLink());
            ps.setTimestamp(4, Timestamp.valueOf(post.getCreated()));
            ps.executeUpdate();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    post.setId(generatedKeys.getInt(1));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Post> getAll() {
        List<Post> result = new ArrayList<>();
        try (PreparedStatement ps = cnn.prepareStatement("SELECT * FROM post")) {
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                result.add(createPost(resultSet));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public Post findById(int id) {
        Post post = null;
        try (PreparedStatement ps = cnn.prepareStatement("SELECT * FROM post WHERE id=?")) {
            ps.setInt(1, id);
            ps.execute();
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                post = createPost(resultSet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return post;
    }

    @Override
    public void close() throws Exception {
        if (cnn != null) {
            cnn.close();
        }
    }

    public Post createPost(ResultSet resultSet) throws SQLException {
        return new Post(resultSet.getInt("ID"), resultSet.getString("NAME"), resultSet.getString("TEXT"), resultSet.getString("LINK"), resultSet.getTimestamp("CREATED").toLocalDateTime());
    }

    public static void main(String[] args) throws SQLException {
        Post post = new Post("java developer", "www.helloWorld.com", "java developer job",
                LocalDateTime.of(2023, 8, 6, 18, 0, 0));
        Post post2 = new Post("java developer", "www.helloWorld.com", "java developer job",
                LocalDateTime.of(2023, 8, 6, 18, 0, 0));
        Post post3 = new Post("java developer", "www.helloWorld1.com", "java developer job",
                LocalDateTime.of(2023, 8, 6, 18, 0, 0));
        PsqlStore psqlStore = new PsqlStore(loadProperties());
        psqlStore.save(post);
        psqlStore.save(post2);
        psqlStore.save(post3);
        System.out.println(psqlStore.getAll());
        System.out.println(psqlStore.findById(8));

    }
}