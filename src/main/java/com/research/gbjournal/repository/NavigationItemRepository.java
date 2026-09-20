package com.research.gbjournal.repository;

import com.research.gbjournal.entity.NavigationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NavigationItemRepository extends JpaRepository<NavigationItem, Long> {
    List<NavigationItem> findByParentIdIsNullOrderByDisplayOrderAsc();
    List<NavigationItem> findByParentIdOrderByDisplayOrderAsc(Long parentId);
    List<NavigationItem> findByParentIdIsNullAndEnabledTrueOrderByDisplayOrderAsc();
    List<NavigationItem> findByParentIdAndEnabledTrueOrderByDisplayOrderAsc(Long parentId);
    List<NavigationItem> findByEnabledTrueOrderByDisplayOrderAsc();
    List<NavigationItem> findAllByOrderByDisplayOrderAsc();
    void deleteByParentId(Long parentId);
}
