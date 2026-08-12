import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Sidebar } from '../sidebar/sidebar';
import { Navbar } from '../navbar/navbar';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, Sidebar, Navbar],
  template: `
    <app-sidebar />
    <div class="main-content">
      <app-navbar />
      <div class="view">
        <router-outlet />
      </div>
    </div>
  `,
})
export class Layout {}
