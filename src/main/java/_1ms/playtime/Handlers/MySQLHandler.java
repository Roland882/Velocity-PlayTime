/*      This file is part of the Velocity Playtime project.
        Copyright (C) 2024-2025 _1ms

        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with this program.  If not, see <https://www.gnu.org/licenses/>. */

package _1ms.playtime.Handlers;

import _1ms.playtime.Main;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class MySQLHandler {

    private final ConfigHandler configHandler;
    private final Main main;
    public HikariDataSource ds;
    public MySQLHandler(ConfigHandler configHandler, Main main) {
        this.configHandler = configHandler;
        this.main = main;
    }

    public boolean openConnection() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mariadb://" + configHandler.getADDRESS() + ":" +  configHandler.getPORT() + "/" + configHandler.getDB_NAME());
            config.setUsername(configHandler.getUSERNAME());
            config.setPassword(configHandler.getPASSWORD());
            config.setDriverClassName("org.mariadb.jdbc.Driver");
            config.setMaximumPoolSize(configHandler.getMaxPoolSize());
            config.setMinimumIdle(configHandler.getMinIdle());
            config.setConnectionTimeout(configHandler.getConnectionTimeout());
            config.setIdleTimeout(configHandler.getIdleTimeout());
            config.setMaxLifetime(configHandler.getMaxLifetime());
            config.setConnectionTestQuery("SELECT 1");
            config.addDataSourceProperty("cachePrepStmts", String.valueOf(configHandler.isCacheStmt()));
            config.addDataSourceProperty("prepStmtCacheSize", String.valueOf(configHandler.getPrepStmtCacheSize()));
            config.addDataSourceProperty("prepStmtCacheSqlLimit", String.valueOf(configHandler.getPrepStmtCacheSqlLimit()));
            config.addDataSourceProperty("useServerPrepStmts", String.valueOf(configHandler.isUseServerPrepStmts()));
            config.addDataSourceProperty("useLocalSessionState", String.valueOf(configHandler.isUseLocalSessionState()));
            config.addDataSourceProperty("cacheServerConfiguration", String.valueOf(configHandler.isCacheServerConfiguration()));
            config.addDataSourceProperty("elideSetAutoCommits", String.valueOf(configHandler.isElideSetAutoCommit()));
            config.addDataSourceProperty("maintainTimeStats", String.valueOf(configHandler.isMaintainTimeStats()));
            ds = new HikariDataSource(config);

            // Create table if it doesn't exist
            try (Connection conn = ds.getConnection();
                 PreparedStatement st = conn.prepareStatement("CREATE TABLE IF NOT EXISTS playtimes (name VARCHAR(20) PRIMARY KEY, time BIGINT NOT NULL)")) {
                st.executeUpdate();
            }
            return true;
        } catch (SQLException sqe) {
            main.getLogger().error("Error while connecting to the database: {}", sqe.getMessage());
            return false;
        }
    }

    public void saveData(final String name, final long time) {
        try (Connection conn = ds.getConnection();
             PreparedStatement st = conn.prepareStatement("INSERT INTO playtimes (name, time) VALUES (?, ?) ON DUPLICATE KEY UPDATE time = ?")) {
            st.setString(1, name);
            st.setLong(2, time);
            st.setLong(3, time);
            st.executeUpdate();
        } catch (SQLException sqe) {
            throw new RuntimeException("Error while saving data into the database", sqe);
        }
    }

    public long readData(final String name) {
        try (Connection conn = ds.getConnection();
             PreparedStatement st = conn.prepareStatement("SELECT TIME FROM playtimes WHERE name = ?")) {
            st.setString(1, name);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("time");
                }
                return -1;
            }
        } catch (SQLException sqe) {
            throw new RuntimeException("Error while reading data from the database", sqe);
        }
    }

    public Iterator<String> getIterator() {
        final Set<String> playtimes = new HashSet<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement st = conn.prepareStatement("SELECT name FROM playtimes");
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                playtimes.add(rs.getString("name"));
            }
            return playtimes.iterator();
        } catch (SQLException sqe) {
            throw new RuntimeException("Error while reading data from the database", sqe);
        }
    }

    public void deleteAll() {
        try (Connection conn = ds.getConnection();
             PreparedStatement st = conn.prepareStatement("DELETE FROM playtimes")) {
            st.executeUpdate();
        } catch (SQLException sqe) {
            throw new RuntimeException("Error while deleting data from the database", sqe);
        }
    }

    public void closeConnection() {
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
    }
}
