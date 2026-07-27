package com.sgs.backend.article;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    boolean existsByCodeArticle(String codeArticle);
}
