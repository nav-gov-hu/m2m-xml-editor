package hu.gov.nav.xsdparsertool.web.database.releaselock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationReleaseLockServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ApplicationReleaseLockService service;

    @BeforeEach
    void setUp() {
        service = new ApplicationReleaseLockService(jdbcTemplate);
    }

    @Test
    void emptyDatabaseLockIsAllowedDuringEarlyValidationWithoutWrite() {
        mockLockedRelease(null);

        service.validateBeforeStartup("4.0.0-20260926-160300-RC1");

        verify(jdbcTemplate, never()).update(anyString(), any(), any(), anyInt());
    }

    @Test
    void emptyDatabaseLockIsClaimedOnlyAtSuccessfulStartupEnd() {
        mockLockedRelease(null);
        when(jdbcTemplate.update(anyString(), any(), any(), anyInt())).thenReturn(1);

        service.claimAfterSuccessfulStartup("4.0.0-20260926-160300-RC1");

        verify(jdbcTemplate).update(anyString(), eq("4.0.0-20260926-160300-RC1"), eq("application-startup"), eq(1));
    }

    @Test
    void sameReleaseIsAcceptedWithoutDatabaseWrite() {
        mockLockedRelease("4.0.0-20260926-160300-RC1");

        service.claimAfterSuccessfulStartup("4.0.0-20260926-160300-RC1");

        verify(jdbcTemplate, never()).update(anyString(), any(), any(), anyInt());
    }

    @Test
    void newerReleaseClaimsDatabaseAfterSuccessfulStartup() {
        mockLockedRelease("4.0.0-20260921-123222-RC1");
        when(jdbcTemplate.update(anyString(), any(), any(), anyInt())).thenReturn(1);

        service.claimAfterSuccessfulStartup("4.0.0-20260926-160300-RC1");

        verify(jdbcTemplate).update(anyString(), eq("4.0.0-20260926-160300-RC1"), eq("application-startup"), eq(1));
    }

    @Test
    void olderReleaseIsRejectedBeforeOtherStartupRunners() {
        mockLockedRelease("4.0.0-20260926-160300-RC1");

        assertThrows(IllegalStateException.class,
                () -> service.validateBeforeStartup("4.0.0-20260921-123222-RC1"));

        verify(jdbcTemplate, never()).update(anyString(), any(), any(), anyInt());
    }

    @Test
    void differentUnsupportedReleaseCannotTakeOverExistingDatabase() {
        mockLockedRelease("4.0.0-20260926-160300-RC1");

        assertThrows(IllegalStateException.class,
                () -> service.validateBeforeStartup("4.0.0-SNAPSHOT-20260927-080000"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void mockLockedRelease(String release) {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1)))
                .thenReturn(Collections.singletonList(release));
    }
}
