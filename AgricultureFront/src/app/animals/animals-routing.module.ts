import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AnimalPageComponent } from './animal-page.component';

const routes: Routes = [
  { path: '', component: AnimalPageComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AnimalsRoutingModule { }
