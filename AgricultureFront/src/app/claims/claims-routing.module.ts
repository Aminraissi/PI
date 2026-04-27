
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { ClaimsLayoutComponent } from './layout/claims-layout.component';
import { MyClaimsComponent }     from './user/my-claims/my-claims.component';
import { NewClaimComponent }     from './user/new-claim/new-claim.component';
import { ClaimsAuthGuard }       from './services/claims-auth.guard';

const routes: Routes = [
  // /claims/auth → redirect to app-level /auth page
  { path: 'auth', redirectTo: '/auth', pathMatch: 'full' },

  // Protected user claims space (layout with topbar)
  {
    path: '',
    component: ClaimsLayoutComponent,
    canActivate: [ClaimsAuthGuard],
    children: [
      { path: '',           redirectTo: 'my-claims', pathMatch: 'full' },
      { path: 'my-claims',  component: MyClaimsComponent },
      { path: 'new',        component: NewClaimComponent },
      {
        path: 'detail/:id',
        loadChildren: () => import('./claim-detail-route.module').then(m => m.ClaimDetailRouteModule)
      },
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ClaimsRoutingModule {}
