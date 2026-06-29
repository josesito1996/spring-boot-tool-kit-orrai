package com.library.support.orrai.data.sample;

import com.library.support.orrai.data.jpa.SoftDeletableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Test fixture: a concrete soft-deletable, auditable entity. */
@Entity
@Table(name = "sample")
public class SampleEntity extends SoftDeletableEntity {

    private String name;

    protected SampleEntity() {
    }

    public SampleEntity(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
