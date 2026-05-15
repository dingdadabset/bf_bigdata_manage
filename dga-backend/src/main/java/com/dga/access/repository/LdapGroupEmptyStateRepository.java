package com.dga.access.repository;

import com.dga.access.entity.LdapGroupEmptyState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LdapGroupEmptyStateRepository extends JpaRepository<LdapGroupEmptyState, Long> {

    LdapGroupEmptyState findByClusterNameAndGroupNameIgnoreCase(String clusterName, String groupName);

    List<LdapGroupEmptyState> findByClusterName(String clusterName);

    void deleteByClusterNameAndGroupNameIgnoreCase(String clusterName, String groupName);
}
