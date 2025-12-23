package com.netflix.util;

import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {
    public static Connection getConnection() throws SQLException {
        ApplicationContext ctx = ApplicationContextProvider.getApplicationContext();
        if (ctx == null) {
            throw new IllegalStateException("Spring context not initialized - ensure application is running");
        }
        DataSource ds = ctx.getBean(DataSource.class);
        return ds.getConnection();
    }
}
