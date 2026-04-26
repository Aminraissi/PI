import { Component } from '@angular/core';

@Component({
  selector: 'app-animal-page',
  standalone: false,
  template: `
    <app-navbar></app-navbar>
    <div class="animal-page-container">
      <app-animal-list></app-animal-list>
    </div>
  `,
  styles: [`
    .animal-page-container {
      padding: 90px 32px 32px 32px;
      min-height: 100vh;
      background: #f4faf4;
    }
  `]
})
export class AnimalPageComponent {}
