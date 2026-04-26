import { ModuleWithProviders, NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HTTP_INTERCEPTORS } from '@angular/common/http';

import { ToastComponent }          from './components/toast/toast.component';
import { AppToastComponent }       from './components/app-toast/app-toast.component';
import { SuccessToastInterceptor } from '../core/interceptors/success-toast.interceptor';

/**
 * ToastModule — à importer dans les modules qui doivent afficher
 * les notifications toast : InventoryModule, ShopModule, AppointmentsModule.
 *
 * Utiliser forRoot() UNE SEULE FOIS (dans le module le plus haut) pour
 * enregistrer l'interceptor HTTP. Les autres modules importent le module
 * sans forRoot() pour obtenir uniquement les composants.
 *
 * Exemple :
 *   InventoryModule  → ToastModule.forRoot()
 *   ShopModule       → ToastModule
 *   AppointmentsModule (importe déjà InventoryModule + ShopModule) → ToastModule
 */
@NgModule({
  declarations: [
    ToastComponent,
    AppToastComponent,
  ],
  imports: [CommonModule],
  exports: [
    ToastComponent,
    AppToastComponent,
  ],
})
export class ToastModule {
  /** Enregistre l'interceptor HTTP — appeler une seule fois au niveau le plus haut. */
  static forRoot(): ModuleWithProviders<ToastModule> {
    return {
      ngModule: ToastModule,
      providers: [
        {
          provide: HTTP_INTERCEPTORS,
          useClass: SuccessToastInterceptor,
          multi: true,
        },
      ],
    };
  }
}
