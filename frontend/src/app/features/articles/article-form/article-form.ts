import { Component, inject, OnInit } from '@angular/core';
import { ArticleService } from '../services/article-service';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CategorieService } from '../../categories/services/categorie-service';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-article-form',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './article-form.html'
})
export class ArticleForm implements OnInit {
  private fb = inject(FormBuilder);
  private articleService = inject(ArticleService);
  protected categorieService = inject(CategorieService); // protected: utilisé dans le template
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  editId: number | null = null;

  form = this.fb.group({
    codeArticle: ['', Validators.required],
    designation: ['', Validators.required],
    prixUnitaireHt: [0, [Validators.required, Validators.min(0)]],
    tauxTva: [19.25, Validators.required],
    photo: [''],
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
      this.articleService.loadAll();
      const existant = this.articleService.articles().find(a => a.id === this.editId);
      if (existant) {
        this.form.patchValue({ ...existant, categorieId: existant.categorie.id });
      }
    }
  }

  enregistrer() {
    if (this.form.invalid) return;
    const dto = this.form.getRawValue() as any;
    const action = this.editId
      ? this.articleService.update(this.editId, dto)
      : this.articleService.create(dto);
    action.subscribe(() => this.router.navigate(['/articles']));
  }
}