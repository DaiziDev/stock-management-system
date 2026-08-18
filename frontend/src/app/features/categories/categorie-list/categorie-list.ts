import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CategorieService } from '../services/categorie-service';

@Component({
  selector: 'app-categorie-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './categorie-list.html'
})
export class CategorieList implements OnInit {
    ngOnInit(): void {
        this.categorieService.loadAll();
    }

    private categorieService = inject(CategorieService);
    categories = this.categorieService.categories;

    supprimer(id: number) {
        this.categorieService.delete(id).subscribe(() => this.categorieService.loadAll())
    }

}