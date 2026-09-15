import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FournisseurService } from '../services/fournisseur-service';

@Component({
  selector: 'app-fournisseur-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './fournisseur-list.html'
})
export class FournisseurList implements OnInit {
    ngOnInit(): void {
        this.fournisseurService.loadAll();
    }

    private fournisseurService = inject(FournisseurService);
    fournisseurs = this.fournisseurService.fournisseurs;

    supprimer(id: number) {
        this.fournisseurService.delete(id).subscribe(() => this.fournisseurService.loadAll())
    }
}
