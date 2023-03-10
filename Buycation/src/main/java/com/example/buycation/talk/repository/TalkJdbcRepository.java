package com.example.buycation.talk.repository;

import com.example.buycation.talk.dto.TalkRedisDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TalkJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public void batchInsert(List<TalkRedisDto> talkRedisDtos){

        String sql = "INSERT INTO talk"
                +"(created_at, message, talk_room_id, member_id)"
                +"VALUES (?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                TalkRedisDto talkRedisDto = talkRedisDtos.get(i);

                ps.setTimestamp(1, Timestamp.valueOf(talkRedisDto.getSendDate()));
                ps.setString(2, talkRedisDto.getMessage());
                ps.setLong(3, talkRedisDto.getTalkRoomId());
                ps.setLong(4, talkRedisDto.getMemberId());
            }

            @Override
            public int getBatchSize() {
                return talkRedisDtos.size();
            }
        });

    }

}
