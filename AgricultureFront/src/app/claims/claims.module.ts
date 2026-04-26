import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

import { ClaimsRoutingModule }    from './claims-routing.module';
import { ClaimsLayoutComponent }  from './layout/claims-layout.component';
import { MyClaimsComponent }      from './user/my-claims/my-claims.component';
import { NewClaimComponent }      from './user/new-claim/new-claim.component';
import { ClaimsSharedModule }     from './claims-shared.module';

@NgModule({
  declarations: [
    ClaimsLayoutComponent,
    MyClaimsComponent,
    NewClaimComponent,
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule,
    ClaimsSharedModule,
    ClaimsRoutingModule,
  ]
})
export class ClaimsModule {}
