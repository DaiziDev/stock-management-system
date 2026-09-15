import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ClientService } from '../services/client-service';

@Component({
  selector: 'app-client-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './client-list.html'
})
export class ClientList implements OnInit {
    ngOnInit(): void {
        this.clientService.loadAll();
    }

    private clientService = inject(ClientService);
    clients = this.clientService.clients;

    supprimer(id: number) {
        this.clientService.delete(id).subscribe(() => this.clientService.loadAll())
    }
}
