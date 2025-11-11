package com.example.userservice.repository;

import com.example.userservice.model.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public UserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public User save(User user) {
        String sql = """
            INSERT INTO users (username, email, first_name, last_name, created_at, updated_at)
            VALUES (:username, :email, :first_name, :last_name, :created_at, :updated_at)
            RETURNING id, username, email, first_name, last_name, created_at, updated_at
            """;

        LocalDateTime now = LocalDateTime.now();
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("username", user.username())
            .addValue("email", user.email())
            .addValue("first_name", user.firstName())
            .addValue("last_name", user.lastName())
            .addValue("created_at", Timestamp.valueOf(now))
            .addValue("updated_at", Timestamp.valueOf(now));

        try {
            return jdbc.queryForObject(sql, params, (rs, rowNum) -> new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
            ));
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage().contains("username")) {
                throw new IllegalArgumentException("Username already exists: " + user.username(), e);
            } else if (e.getMessage().contains("email")) {
                throw new IllegalArgumentException("Email already exists: " + user.email(), e);
            }
            throw e;
        }
    }

    public Optional<User> findById(Long id) {
        String sql = """
            SELECT id, username, email, first_name, last_name, created_at, updated_at
            FROM users
            WHERE id = :id
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", id);

        try {
            User user = jdbc.queryForObject(sql, params, (rs, rowNum) -> new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
            ));
            return Optional.ofNullable(user);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findByUsername(String username) {
        String sql = """
            SELECT id, username, email, first_name, last_name, created_at, updated_at
            FROM users
            WHERE username = :username
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("username", username);

        try {
            User user = jdbc.queryForObject(sql, params, (rs, rowNum) -> new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
            ));
            return Optional.ofNullable(user);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findByEmail(String email) {
        String sql = """
            SELECT id, username, email, first_name, last_name, created_at, updated_at
            FROM users
            WHERE email = :email
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("email", email);

        try {
            User user = jdbc.queryForObject(sql, params, (rs, rowNum) -> new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
            ));
            return Optional.ofNullable(user);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<User> findAll() {
        String sql = """
            SELECT id, username, email, first_name, last_name, created_at, updated_at
            FROM users
            ORDER BY created_at DESC
            """;

        return jdbc.query(sql, (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("email"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
        ));
    }
}

