package com.transproj.repo;

import com.transproj.domain.TranslationJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranslationJobRepository extends JpaRepository<TranslationJob, String> {
}
