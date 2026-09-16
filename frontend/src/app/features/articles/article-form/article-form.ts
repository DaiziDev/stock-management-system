import { Component, inject, OnInit } from '@angular/core';
import { ArticleService } from '../services/article-service';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CategorieService } from '../../categories/services/categorie-service';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AppIcon } from '../../../shared/components/icon/icon';
import type { ArticleRequest } from '../models/article.model';

@Component({
  selector: 'app-article-form',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, RouterLink, AppIcon],
  templateUrl: './article-form.html'
})
export class ArticleForm implements OnInit {
  private fb = inject(FormBuilder);
  private articleService = inject(ArticleService);
  protected categorieService = inject(CategorieService); // protected: utilisé dans le template
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  editId: number | null = null;
  chargement = false;
  erreur: string | null = null;

  form = this.fb.group({
    codeArticle: ['', Validators.required],
    designation: ['', Validators.required],
    prixUnitaireHt: [0, [Validators.required, Validators.min(0)]],
    tauxTva: [19.25, Validators.required],
    photo: [''],
    seuilMin: [null as number | null, Validators.min(0)],
    categorieId: [null as number | null, Validators.required],
  });

  ngOnInit() {
    // on charge les catégories nous-mêmes ici, sans supposer qu'elles
    // sont déjà en mémoire — l'utilisateur peut arriver directement sur
    // cette URL sans être passé par la liste des catégories avant
    this.categorieService.loadAll();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.editId = Number(idParam);
      this.chargement = true;
      this.articleService.findById(this.editId).subscribe({
        next: (existant) => {
          this.form.patchValue({ ...existant, categorieId: existant.categorie.id });
          this.chargement = false;
        },
        error: () => {
          this.erreur = 'Cet article est introuvable ou n’est plus accessible.';
          this.chargement = false;
        },
      });
    }
  }

  enregistrer() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const dto: ArticleRequest = {
      codeArticle: raw.codeArticle ?? '',
      designation: raw.designation ?? '',
      prixUnitaireHt: Number(raw.prixUnitaireHt),
      tauxTva: Number(raw.tauxTva),
      photo: raw.photo?.trim() || null,
      seuilMin: raw.seuilMin === null || raw.seuilMin === undefined ? null : Number(raw.seuilMin),
      categorieId: Number(raw.categorieId),
    };
    const action = this.editId
      ? this.articleService.update(this.editId, dto)
      : this.articleService.create(dto);
    action.subscribe({
      next: () => this.router.navigate(['/articles']),
      error: (error) => this.erreur = error?.error?.message ?? 'Impossible d’enregistrer cet article.',
    });
  }
}
