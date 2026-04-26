import { Component, Input } from '@angular/core';
import { ClimateMonthSummary, WeatherForecastDay } from '../../models/calendar.model';
import { Terrain } from '../../models/terrain.model';

interface TunisiaWeatherMarker {
  top: number;
  left: number;
  temp: number;
  icon: 'sun' | 'cloud-sun' | 'cloud';
}

interface TunisiaPlaceLabel {
  top: number;
  left: number;
  name: string;
  emphasis?: boolean;
  align?: 'left' | 'center' | 'right';
}

@Component({
  selector: 'app-farm-calendar-climate',
  standalone: false,
  templateUrl: './farm-calendar-climate.component.html',
  styleUrls: ['./farm-calendar-climate.component.css']
})
export class FarmCalendarClimateComponent {
  @Input() terrains: Terrain[] = [];
  @Input() selectedTerrainId: number | null = null;
  @Input() selectedTerrain: Terrain | null = null;
  @Input() climateYear = '';
  @Input() climateLoading = false;
  @Input() climateMonthly: ClimateMonthSummary[] = [];
  @Input() weather: WeatherForecastDay[] = [];
  activeMapTab: 'weather' | 'wind' = 'weather';
  activeDay = 'today';
  activePeriod = 'morning';

  readonly tunisiaZones = [
    {
      key: 'north',
      label: 'North',
      summary: 'Higher humidity, more regular rainfall, and stronger storm and wind exposure.'
    },
    {
      key: 'center',
      label: 'Center',
      summary: 'Transition belt with mixed rainfall patterns and moderate water stress.'
    },
    {
      key: 'south',
      label: 'South',
      summary: 'More arid conditions with stronger heat and sand-risk sensitivity.'
    }
  ];

  readonly weatherMarkers: TunisiaWeatherMarker[] = [
    { top: 10, left: 52, temp: 19, icon: 'cloud-sun' },
    { top: 14, left: 35, temp: 18, icon: 'cloud-sun' },
    { top: 16, left: 63, temp: 19, icon: 'cloud-sun' },
    { top: 19, left: 76, temp: 18, icon: 'cloud-sun' },
    { top: 25, left: 34, temp: 17, icon: 'cloud-sun' },
    { top: 24, left: 48, temp: 18, icon: 'cloud-sun' },
    { top: 23, left: 60, temp: 19, icon: 'cloud-sun' },
    { top: 28, left: 71, temp: 20, icon: 'cloud-sun' },
    { top: 36, left: 35, temp: 17, icon: 'cloud-sun' },
    { top: 35, left: 53, temp: 21, icon: 'cloud' },
    { top: 40, left: 70, temp: 20, icon: 'cloud' },
    { top: 49, left: 36, temp: 20, icon: 'cloud-sun' },
    { top: 53, left: 21, temp: 21, icon: 'cloud-sun' },
    { top: 54, left: 62, temp: 20, icon: 'sun' },
    { top: 62, left: 45, temp: 21, icon: 'cloud-sun' },
    { top: 61, left: 70, temp: 20, icon: 'cloud-sun' },
    { top: 73, left: 63, temp: 18, icon: 'cloud-sun' },
    { top: 92, left: 52, temp: 28, icon: 'sun' }
  ];

  readonly placeLabels: TunisiaPlaceLabel[] = [
    { top: 7, left: 55, name: 'Bizerte', align: 'center' },
    { top: 17, left: 36, name: 'Beja', align: 'left' },
    { top: 23, left: 58, name: 'Tunis', emphasis: true, align: 'center' },
    { top: 29, left: 73, name: 'Nabeul', align: 'left' },
    { top: 32, left: 46, name: 'Kairouan', emphasis: true, align: 'left' },
    { top: 41, left: 73, name: 'Sousse', align: 'left' },
    { top: 50, left: 66, name: 'Sfax', align: 'left' },
    { top: 64, left: 39, name: 'Gafsa', align: 'center' },
    { top: 73, left: 14, name: 'Tozeur', align: 'left' },
    { top: 77, left: 56, name: 'Gabes', align: 'center' },
    { top: 92, left: 58, name: 'Medenine', align: 'center' },
    { top: 98, left: 53, name: 'Tataouine', align: 'center' }
  ];

  get selectedZoneKey(): string {
    const latitude = this.selectedTerrain?.latitude ?? 35;

    if (latitude >= 35.5) {
      return 'north';
    }

    if (latitude >= 33) {
      return 'center';
    }

    return 'south';
  }

  get selectedZoneLabel(): string {
    return this.tunisiaZones.find((zone) => zone.key === this.selectedZoneKey)?.label ?? 'Center';
  }

  get totalRain(): number {
    return this.climateMonthly.reduce((sum, row) => sum + row.totalRainMm, 0);
  }

  get averageMin(): number {
    if (!this.climateMonthly.length) {
      return 0;
    }

    return this.climateMonthly.reduce((sum, row) => sum + row.avgTempMin, 0) / this.climateMonthly.length;
  }

  get averageMax(): number {
    if (!this.climateMonthly.length) {
      return 0;
    }

    return this.climateMonthly.reduce((sum, row) => sum + row.avgTempMax, 0) / this.climateMonthly.length;
  }

  get wettestMonth(): ClimateMonthSummary | null {
    if (!this.climateMonthly.length) {
      return null;
    }

    return this.climateMonthly.reduce((wettest, row) => (
      row.totalRainMm > wettest.totalRainMm ? row : wettest
    ));
  }

  get warmestMonth(): ClimateMonthSummary | null {
    if (!this.climateMonthly.length) {
      return null;
    }

    return this.climateMonthly.reduce((warmest, row) => (
      row.avgTempMax > warmest.avgTempMax ? row : warmest
    ));
  }

  get selectedMarkerStyle(): { [key: string]: string } {
    const terrain = this.selectedTerrain;
    if (!terrain) {
      return { top: '43%', left: '56%' };
    }

    const latMin = 30.2;
    const latMax = 37.4;
    const lonMin = 7.4;
    const lonMax = 11.7;
    const top = ((latMax - terrain.latitude) / (latMax - latMin)) * 100;
    const left = ((terrain.longitude - lonMin) / (lonMax - lonMin)) * 100;

    return {
      top: `${Math.min(88, Math.max(10, top))}%`,
      left: `${Math.min(86, Math.max(22, left))}%`
    };
  }

  getRainBarHeight(row: ClimateMonthSummary): number {
    const maxRain = Math.max(...this.climateMonthly.map((item) => item.totalRainMm), 1);
    return Math.max(12, (row.totalRainMm / maxRain) * 100);
  }

  get mapDateLabel(): string {
    return this.weather.length ? this.weather[0].date : 'Today';
  }

  setMapTab(tab: 'weather' | 'wind'): void {
    this.activeMapTab = tab;
  }

  setActiveDay(day: string): void {
    this.activeDay = day;
  }

  setActivePeriod(period: string): void {
    this.activePeriod = period;
  }

  isWeatherView(): boolean {
    return this.activeMapTab === 'weather';
  }

  displayIcon(icon: TunisiaWeatherMarker['icon']): TunisiaWeatherMarker['icon'] | 'wind' {
    return this.activeMapTab === 'wind' ? 'wind' : icon;
  }

  iconClass(icon: TunisiaWeatherMarker['icon'] | 'wind'): string {
    switch (icon) {
      case 'sun':
        return 'fas fa-sun';
      case 'cloud':
        return 'fas fa-cloud';
      case 'wind':
        return 'fas fa-wind';
      default:
        return 'fas fa-cloud-sun';
    }
  }
}
