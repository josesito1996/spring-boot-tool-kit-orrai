package com.library.support.orrai.data.sample;

import com.library.support.orrai.data.jpa.AuditableFields;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Test fixture for {@code AuditableFields}: an entity that owns its primary key (an {@code id} field
 * declared here) and inherits only the audit columns. This reproduces the scenario that fails when
 * extending {@code BaseEntity} (duplicate {@code @Id}) and proves it works with {@code AuditableFields}.
 */
@Entity
@Table(name = "custom_id_auditable")
public class CustomIdAuditableEntity extends AuditableFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    protected CustomIdAuditableEntity() {
    }

    public CustomIdAuditableEntity(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}