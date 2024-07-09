package com.ghostchu.peerbanhelper.database;

import com.ghostchu.peerbanhelper.Main;
import com.j256.ormlite.jdbc.JdbcSingleConnectionSource;
import com.j256.ormlite.jdbc.db.SqliteDatabaseType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.SQLException;

@Getter
@Component
public class Database {
    private JdbcSingleConnectionSource dataSource;
    private HikariDataSource hikari;

    public Database() throws SQLException {
        File databaseDirectory = new File(Main.getDataDirectory(), "persist");
        if (!databaseDirectory.exists()) {
            databaseDirectory.mkdirs();
        }
        File sqliteDb = new File(databaseDirectory, "peerbanhelper.db");
        setupDatabase(sqliteDb);
    }

    public void setupDatabase(File file) throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setPoolName("PeerBanHelper SQLite Connection Pool");
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + file);
        config.setConnectionTestQuery("SELECT 1");
        config.setMaxLifetime(60000); // 60 Sec
        config.setMaximumPoolSize(1); // 50 Connections (including idle connections)
        this.hikari = new HikariDataSource(config);
        this.dataSource = new JdbcSingleConnectionSource("jdbc:sqlite:" + file, new SqliteDatabaseType(), this.hikari.getConnection());

        //  this.dataSource = new DataSourceConnectionSource( new HikariDataSource(config), new SqliteDatabaseType());
    }

    public void close() {
        this.dataSource.closeQuietly();
    }
}