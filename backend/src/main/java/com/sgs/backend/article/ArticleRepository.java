package com.sgs.backend.article;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    boolean existsByCodeArticle(String codeArticle);

    List<Article> findByEntrepriseId(Long entrepriseId);
}
