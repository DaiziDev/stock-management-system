import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { CategorieService } from '../services/categorie-service';
import { CategorieRequest } from '../models/categorie.model';

@Component({
  selector: 'app-categorie-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './categorie-form.html',
})
export class CategorieForm implements OnInit {
  private fb = inject(FormBuilder);
  private categorieService = inject(CategorieService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  editId: number | null = null;

  form = this.fb.group({
    code: ['', Validators.required],
    designation: ['', Validators.required],
  });

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.editId = Number(idParam);
      const existant = this.categorieService.categories().find((c) => c.id === this.editId);
      if (existant) this.form.patchValue(existant);
    }
  }

  enregistrer() {
    if (this.form.invalid) return;
    const dto = this.form.getRawValue() as CategorieRequest;
    const action = this.editId
      ? this.categorieService.update(this.editId, dto)
      : this.categorieService.create(dto);
    action.subscribe(() => this.router.navigate(['/categories']));
  }
}
