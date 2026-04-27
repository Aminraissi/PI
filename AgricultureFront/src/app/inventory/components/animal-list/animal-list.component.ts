import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { Router } from '@angular/router';
import { InventoryApiService } from '../../services/inventory-api.service';
import { Animal } from '../../models/inventory.models';

@Component({
  selector: 'app-animal-list',
  standalone: false,
  templateUrl: './animal-list.component.html',
  styleUrls: ['./animal-list.component.css']
})
export class AnimalListComponent implements OnInit, OnChanges {
  @Input() initialView: 'list' | 'campaigns' = 'list';
  @Output() viewChanged = new EventEmitter<'list' | 'campaigns'>();

  animals: Animal[] = [];
  loading = true;
  error   = '';

  // Sub-views
  view: 'list' | 'campaigns' = 'list';
  selectedAnimal: Animal | null = null;

  // Modals
  showAnimalForm      = false;
  showAnimalDetail    = false;
  showCampaignForm    = false;
  showVaccModal       = false;   // vaccination individuelle
  editingAnimal: Animal | null = null;

  constructor(private api: InventoryApiService, private router: Router) {}

  ngOnInit() {
    this.view = this.initialView;
    this.load();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['initialView'] && !changes['initialView'].firstChange) {
      this.view = changes['initialView'].currentValue;
      this.viewChanged.emit(this.view);
    }
  }

  load() {
    this.loading = true;
    this.error   = '';
    this.api.getMyAnimals().subscribe({
      next: a => { this.animals = a; this.loading = false; },
      error: (e) => {
        this.loading = false;
        if (e.status === 0) {
          this.error = 'Server inaccessible (port 8088).';
        } else {
          this.error = e.error?.message || 'Error loading animals';
        }
      }
    });
  }

  openAdd()  { this.editingAnimal = null; this.showAnimalForm = true; }
  openEdit(a: Animal) { this.editingAnimal = a; this.showAnimalForm = true; }
  onSaved()  { this.showAnimalForm = false; this.load(); }

  delete(a: Animal) {
    if (!confirm(`Delete the animal "${a.reference}" ?`)) return;
    this.api.deleteAnimal(a.id).subscribe({ next: () => this.load() });
  }

  openDetail(a: Animal)   { this.selectedAnimal = a; this.showAnimalDetail = true; }
  openVaccine(a: Animal)  { this.selectedAnimal = a; this.showVaccModal = true; }

  openCampaigns() { this.view = 'campaigns'; this.viewChanged.emit(this.view); }
  backToList()    { this.view = 'list'; this.viewChanged.emit(this.view); }

  age(dateNaissance: string): number {
    const now  = new Date();
    const born = new Date(dateNaissance);
    return Math.floor((now.getTime() - born.getTime()) / (1000 * 60 * 60 * 24 * 365.25));
  }

  goToAppointments() { this.router.navigate(['/appointments']); }
  goToBookRdv()      { localStorage.setItem('apptDefaultView','book'); this.router.navigate(['/appointments']); }
}
