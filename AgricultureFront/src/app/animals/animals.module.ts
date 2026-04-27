import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AnimalsRoutingModule } from './animals-routing.module';
import { InventoryModule } from '../inventory/inventory.module';
import { SharedModule } from '../shared/shared.module';
import { AnimalPageComponent } from './animal-page.component';

@NgModule({
  declarations: [AnimalPageComponent],
  imports: [
    CommonModule,
    SharedModule,
    AnimalsRoutingModule,
    InventoryModule
  ]
})
export class AnimalsModule { }
