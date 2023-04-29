package fotius.example.donations.webhook.infra.jdbc;

import fotius.example.donations.payment.domain.model.PaymentMethod;
import fotius.example.donations.payment.domain.model.PaymentState;
import fotius.example.donations.webhook.domain.WebhookRepository;
import fotius.example.donations.webhook.domain.model.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Repository
public class JdbcWebhookRepository implements WebhookRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void insert(Webhook webhook) {
        final GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                """
                INSERT INTO webhook (
                    user_id,
                    target_url,
                    payment_method,
                    payment_state,
                    
                ) VALUES (
                    :user_id,
                    :target_url,
                    :payment_method,
                    :payment_state,
                    
                )
                """,
                new MapSqlParameterSource()
                        .addValue("user_id", webhook.getUserId())
                        .addValue("target_url", webhook.getTargetUrl())
                        .addValue("payment_method", webhook.getPaymentMethod().name())
                        .addValue("payment_state", webhook.getPaymentState().name()),

                keyHolder
        );
        webhook.setId(keyHolder.getKey().longValue());
    }

    @Override
    public void delete(Long id) {
        jdbcTemplate.update(
        """
            DELETE FROM webhook
                WHERE id = :id;
            """,
            new MapSqlParameterSource()
                    .addValue("id", id)
        );
    }

    @Override
    public List<Webhook> getWithMethodAndState(PaymentMethod method, PaymentState state) {
        return jdbcTemplate.query(
            """
                SELECT * FROM webhook
                    WHERE payment_method=:payment_method AND payment_state=:payment_state;
                """,
            new MapSqlParameterSource()
                    .addValue("payment_method", method.name())
                    .addValue("payment_state", state.name()),
                JdbcWebhookRepository::mapRow
        );
    }

    @Override
    public List<Webhook> getAllByUserId(Long userId) {
        return jdbcTemplate.query(
            """
                SELECT * FROM webhook
                WHERE user_id=:user_id;
                """,
            new MapSqlParameterSource()
                    .addValue("user_id", userId),
                JdbcWebhookRepository::mapRow
        );
    }

    private static Webhook mapRow(ResultSet rs, int rowNum) {
        try {
            return Webhook.builder()
                    .id(rs.getLong("id"))
                    .userId(rs.getLong("user_id"))
                    .targetUrl(Objects.isNull(rs.getString("target_url")) ? null : new URL(rs.getString("target_url")))
                    .paymentMethod(PaymentMethod.valueOf(rs.getString("payment_method")))
                    .paymentState(PaymentState.valueOf(rs.getString("payment_state")))
                    .build();
        } catch (MalformedURLException | SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
