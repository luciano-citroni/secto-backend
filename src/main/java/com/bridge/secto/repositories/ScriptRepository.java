package com.bridge.secto.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bridge.secto.entities.Script;

@Repository
public interface ScriptRepository extends JpaRepository<Script, UUID> {
    java.util.List<Script> findByServiceSubTypeId(UUID serviceSubTypeId);
}
