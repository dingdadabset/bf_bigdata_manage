package com.dga.access.service;

import com.dga.access.dto.OffboardingRevocationPreview;
import com.dga.access.dto.OffboardingRevocationRequest;
import com.dga.access.dto.OffboardingRevocationTaskView;
import com.dga.access.entity.DgaUser;
import com.dga.access.entity.OffboardingRevocationTask;
import com.dga.access.repository.DgaUserRepository;
import com.dga.access.repository.OffboardingRevocationTaskRepository;
import com.dga.access.repository.UserResourceAccessRepository;
import com.dga.access.service.authorization.AuthorizationService;
import com.dga.settings.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OffboardingRevocationServiceTest {

    @Mock
    private DgaUserRepository dgaUserRepository;

    @Mock
    private OffboardingRevocationTaskRepository taskRepository;

    @Mock
    private UserResourceAccessRepository userResourceAccessRepository;

    @Mock
    private LdapService ldapService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AccessGovernanceService accessGovernanceService;

    private OffboardingRevocationService service;

    @BeforeEach
    void setUp() {
        service = new OffboardingRevocationService();
        ReflectionTestUtils.setField(service, "dgaUserRepository", dgaUserRepository);
        ReflectionTestUtils.setField(service, "taskRepository", taskRepository);
        ReflectionTestUtils.setField(service, "userResourceAccessRepository", userResourceAccessRepository);
        ReflectionTestUtils.setField(service, "ldapService", ldapService);
        ReflectionTestUtils.setField(service, "authorizationService", authorizationService);
        ReflectionTestUtils.setField(service, "notificationService", notificationService);
        ReflectionTestUtils.setField(service, "accessGovernanceService", accessGovernanceService);
    }

    @Test
    void previewBlocksWhenActiveTaskAlreadyExists() {
        DgaUser user = user("legacy_user", "CDH");
        OffboardingRevocationTask activeTask = task(1L, "OFFBOARD-TASK-1", "legacy_user", "CDH", "PENDING");
        OffboardingRevocationRequest request = request("legacy_user", "CDH");

        when(dgaUserRepository.findByUsernameAndClusterNameAndIsDeletedFalse("legacy_user", "CDH")).thenReturn(user);
        when(taskRepository.findFirstByUsernameAndClusterAndStatusInOrderByCreatedAtDesc(
                eq("legacy_user"), eq("CDH"), any())).thenReturn(activeTask);
        when(authorizationService.engineType("CDH")).thenReturn("HIVE");
        when(ldapService.userExists("CDH", "legacy_user")).thenReturn(true);
        when(authorizationService.getUserPermissions("legacy_user", "CDH"))
                .thenReturn(Collections.singletonList(new HashMap<String, Object>()));
        when(userResourceAccessRepository.countActiveByUsernameAndCluster("legacy_user", "CDH")).thenReturn(2L);

        OffboardingRevocationPreview preview = service.preview(request);

        assertThat(preview.isExecutable()).isFalse();
        assertThat(preview.isActiveTaskExists()).isTrue();
        assertThat(preview.getActiveTaskNo()).isEqualTo("OFFBOARD-TASK-1");
        assertThat(preview.getBlockers()).anyMatch(item -> item.contains("已有待执行或执行中"));
        assertThat(preview.getLivePermissionCount()).isEqualTo(1);
        assertThat(preview.getRecordedPermissionCount()).isEqualTo(2L);
    }

    @Test
    void listTasksOmitsConfirmationText() {
        OffboardingRevocationTask task = task(2L, "OFFBOARD-TASK-2", "alice", "CDH", "COMPLETED");
        task.setConfirmationText("confirmation body");
        when(taskRepository.findByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(Collections.singletonList(task));

        List<OffboardingRevocationTaskView> tasks = service.listTasks(null, 10);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getTaskNo()).isEqualTo("OFFBOARD-TASK-2");
        assertThat(tasks.get(0).getConfirmationText()).isNull();
    }

    @Test
    void getTaskIncludesConfirmationText() {
        OffboardingRevocationTask task = task(3L, "OFFBOARD-TASK-3", "bob", "CDH", "COMPLETED");
        task.setConfirmationText("confirmation body");
        when(taskRepository.findById(3L)).thenReturn(java.util.Optional.of(task));

        OffboardingRevocationTaskView view = service.getTask(3L);

        assertThat(view.getTaskNo()).isEqualTo("OFFBOARD-TASK-3");
        assertThat(view.getConfirmationText()).isEqualTo("confirmation body");
    }

    private OffboardingRevocationRequest request(String username, String cluster) {
        OffboardingRevocationRequest request = new OffboardingRevocationRequest();
        request.setUsername(username);
        request.setCluster(cluster);
        request.setDepartureDate(LocalDate.now());
        request.setManagerContact("manager");
        request.setSecurityContact("security");
        return request;
    }

    private DgaUser user(String username, String cluster) {
        DgaUser user = new DgaUser();
        user.setUsername(username);
        user.setClusterName(cluster);
        user.setCreationStrategy("LDAP_IMPORT");
        user.setProtectedUser(false);
        return user;
    }

    private OffboardingRevocationTask task(Long id, String taskNo, String username, String cluster, String status) {
        OffboardingRevocationTask task = new OffboardingRevocationTask();
        task.setId(id);
        task.setTaskNo(taskNo);
        task.setUsername(username);
        task.setCluster(cluster);
        task.setDepartureDate(LocalDate.now());
        task.setScheduledAt(LocalDateTime.now());
        task.setStatus(status);
        task.setCreatedBy("admin");
        task.setCreatedAt(LocalDateTime.now());
        task.setMessage("message");
        return task;
    }
}
