import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ClientService } from '../services/client-service';
import { ClientRequest } from '../models/client.model';

@Component({
  selector: 'app-client-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './client-form.html',
})
export class ClientForm implements OnInit {
  private fb = inject(FormBuilder);
  private clientService = inject(ClientService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  editId: number | null = null;

  form = this.fb.group({
    nom: ['', Validators.required],
    prenom: ['', Validators.required],
    adresse1: [''],
    adresse2: [''],
    ville: [''],
    codePostal: [''],
    pays: [''],
    mail: [''],
    numTel: [''],
    photo: [''],
  });

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.editId = Number(idParam);
      const existant = this.clientService.clients().find((c) => c.id === this.editId);
      if (existant) this.form.patchValue(existant);
    }
  }

  enregistrer() {
    if (this.form.invalid) return;
    const dto = this.form.getRawValue() as ClientRequest;
    const action = this.editId
      ? this.clientService.update(this.editId, dto)
      : this.clientService.create(dto);
    action.subscribe(() => this.router.navigate(['/clients']));
  }
}
