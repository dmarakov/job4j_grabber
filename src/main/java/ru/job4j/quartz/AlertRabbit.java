package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Properties;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;

public class AlertRabbit {
    private static final Logger LOG = LoggerFactory.getLogger(AlertRabbit.class.getName());

    public static void main(String[] args) {
        try {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            JobDataMap data = new JobDataMap();
            data.put("connection", initConnection());
            JobDetail job = newJob(Rabbit.class).usingJobData(data).build();
            SimpleScheduleBuilder times = simpleSchedule()
                    .withIntervalInSeconds(Integer.parseInt(loadProperties().getProperty("rabbit.interval")))
                    .repeatForever();
            Trigger trigger = newTrigger()
                    .startNow()
                    .withSchedule(times)
                    .build();
            scheduler.scheduleJob(job, trigger);
            Thread.sleep(10000);
            scheduler.shutdown();
        } catch (SchedulerException | SQLException | ClassNotFoundException | InterruptedException se) {
            LOG.error("Exception", se);
        }
    }

    public static Properties loadProperties() {
        Properties config = new Properties();
        try (InputStream in = AlertRabbit.class.getClassLoader().getResourceAsStream("rabbit.properties")) {
            config.load(in);
        } catch (Exception e) {
            LOG.error("Exception", e);
        }
        return config;
    }

    private static Connection initConnection() throws ClassNotFoundException, SQLException {
        Class.forName(loadProperties().getProperty("jdbc.driver"));
        String url = loadProperties().getProperty("jdbc.url");
        String login = loadProperties().getProperty("jdbc.username");
        String password = loadProperties().getProperty("jdbc.password");
        return DriverManager.getConnection(url, login, password);
    }

    public static class Rabbit implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            System.out.println("Rabbit runs here ...");
            Connection Connection = (Connection) context.getJobDetail().getJobDataMap().get("connection");
            try (PreparedStatement ps = Connection.prepareStatement("INSERT INTO rabbit(created_date) VALUES(?)")) {
                ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                ps.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}