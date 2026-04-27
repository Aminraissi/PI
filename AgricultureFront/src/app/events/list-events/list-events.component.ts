import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { EventListItem } from 'src/app/models/event-list.model';
import { Events } from 'src/app/models/events';
import { EventService } from 'src/app/services/event/event.service';

@Component({
  selector: 'app-list-events',
  templateUrl: './list-events.component.html',
  styleUrls: ['./list-events.component.css']
})
export class ListComponent implements OnInit {

  events: EventListItem[] = [];
  page = 0;
  size = 6;

 selectedType: string = '';
 selectedRegion: string = '';


  loading: boolean = true;

  constructor(private eventService: EventService, private router: Router ) {}

 ngOnInit(): void {
  this.loadEvents();
}

loadEvents(): void {
  this.loading = true;

  this.eventService.getValidatedEventsFiltered(this.page, this.size, this.selectedType, this.selectedRegion)
    .subscribe({
      next: (data) => {
        this.events = data.content;
        this.loading = false;
      },
      error: () => this.loading = false
    });
}

onFilterChange(): void {
  this.page = 0; 
  this.loadEvents();
}
nextPage(): void {
  this.page++;
  this.loadEvents();
}

prevPage(): void {
  if (this.page > 0) {
    this.page--;
    this.loadEvents();
  }
}
  
  goToDetails(id: number) {
    this.router.navigate(['/events/detailsEvent', id]); 
    window.scrollTo(0, 0);
  }

  getImageUrl(filename: string): string {
  return this.eventService.getImageUrl(filename);
}

  openMapView(): void {
    this.router.navigate(['/events/map']);
  }

  openMapForEvent(eventId: number): void {
    this.router.navigate(['/events/map'], { queryParams: { focusId: eventId } });
  }

  getPourcentage(inscrits: number, capacite: number): number {
    return (inscrits || 0) / capacite * 100;
  }

  getCouleur(inscrits: number, capacite: number): string {
    const pourcentage = this.getPourcentage(inscrits, capacite);
    
    if (pourcentage == 100) return '#dc3545';      
    if (pourcentage >= 70 && pourcentage < 99) return '#dbbf0a';      
    return '#3cb054';                             
  }

  
}