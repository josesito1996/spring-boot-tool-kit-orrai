package com.library.support.orrai.data.sample;

import com.library.support.orrai.data.repository.SoftDeleteRepository;

/** Test fixture repository exercising {@code SoftDeleteRepository}. */
public interface SampleRepository extends SoftDeleteRepository<SampleEntity, Long> {
}
