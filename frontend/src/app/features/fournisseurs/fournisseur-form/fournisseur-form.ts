import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { FournisseurService } from '../services/fournisseur-service';
import { FournisseurRequest } from '../models/fournisseur.model';

@Component({
  selector: 'app-fournisseur-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './fournisseur-form.html',
})
export class FournisseurForm implements OnInit {
  private fb = inject(FormBuilder);
  private fournisseurService = inject(FournisseurService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  editId: number | null = null;

  form = this.fb.group({
    nom: ['', Validators.required],
    adresse1: [''],
    adresse2: [''],
    ville: [''],
    codePostal: [''],
    pays: [''],
    mail: [''],
    numTel: [''],
  });

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.editId = Number(idParam);
      const existant = this.fournisseurService.fournisseurs().find((f) => f.id === this.editId);
      if (existant) this.form.patchValue(existant);
    }
  }

  enregistrer() {
    if (this.form.invalid) return;
    const dto = this.form.getRawValue() as FournisseurRequest;
    const action = this.editId
      ? this.fournisseurService.update(this.editId, dto)
      : this.fournisseurService.create(dto);
    action.subscribe(() => this.router.navigate(['/fournisseurs']));
  }
}
